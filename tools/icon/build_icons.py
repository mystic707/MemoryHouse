"""앱 런처 아이콘과 앱 내 로고 생성기.

tools/icon/geom.py 의 좌표로 아래를 만든다. 저장소 루트에서 실행할 것.

    python3 tools/icon/build_icons.py

  drawable/ic_launcher_background.xml   적응형 아이콘 배경 (API 26+)
  drawable/ic_launcher_foreground.xml   적응형 아이콘 전경
  drawable/ic_launcher_monochrome.xml   테마 아이콘 (Android 13+)
  drawable/ic_app_logo.xml              앱 내 로고 (잠금 화면)
  mipmap-*/ic_launcher{,_round}.webp    레거시 비트맵 (API 24~25)
"""
import io
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import geom as g
from PIL import Image, ImageDraw

RES = "app/src/main/res"
HDR = ('<?xml version="1.0" encoding="utf-8"?>\n'
       '<!-- tools/icon/build_icons.py 로 생성. 도형 좌표는 tools/icon/geom.py 에 있다. -->\n')

VECTOR_OPEN = ('<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    android:width="108dp"\n'
               '    android:height="108dp"\n'
               '    android:viewportWidth="108"\n'
               '    android:viewportHeight="108">')


def album_paths(a):
    """앨범 도형 6개를 그리는 순서대로."""
    return f'''    <!-- 뒤에 겹쳐진 사진 -->
    <path android:fillColor="{g.SHEET}"
        android:pathData="{g.rrect_path(*a.sheet, a.sheet_r)}" />
    <!-- 앨범 표지 -->
    <path android:fillColor="{g.CARD}"
        android:pathData="{g.rrect_path(*a.card, a.card_r)}" />
    <!-- 앨범 등 -->
    <path android:fillColor="{g.SPINE}"
        android:pathData="{a.spine_path()}" />
    <!-- 표지 안쪽 사진 -->
    <path android:fillColor="{g.WINDOW}"
        android:pathData="{g.rect_path(*a.window)}" />
    <!-- 해 -->
    <path android:fillColor="{g.SUN}"
        android:pathData="{g.circle_path(*a.sun)}" />
    <!-- 산 -->
    <path android:fillColor="{g.MOUNTAIN}"
        android:pathData="{g.poly_path(a.mountain)}" />'''


def sky_gradient(path_data):
    return f'''    <path android:pathData="{path_data}">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="54" android:startY="0"
                android:endX="54" android:endY="108">
                <item android:offset="0" android:color="#FF{g.BG_TOP[1:]}" />
                <item android:offset="1" android:color="#FF{g.BG_BOTTOM[1:]}" />
            </gradient>
        </aapt:attr>
    </path>'''


def write_vectors():
    launcher = g.launcher_art()
    logo = g.logo_art()

    bg = HDR + f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{sky_gradient("M0,0h108v108h-108z")}
</vector>
'''

    fg = HDR + VECTOR_OPEN + "\n" + album_paths(launcher) + "\n</vector>\n"

    mono = HDR + VECTOR_OPEN + f'''
    <!-- 테마 아이콘은 한 색으로 눌리므로 선으로 형태만 남긴다 -->
    <path android:strokeColor="#000000" android:strokeWidth="3"
        android:fillColor="#00000000"
        android:pathData="{g.rrect_path(*launcher.sheet, launcher.sheet_r)}" />
    <path android:strokeColor="#000000" android:strokeWidth="3.5"
        android:fillColor="#00000000"
        android:pathData="{g.rrect_path(*launcher.card, launcher.card_r)}" />
    <path android:fillColor="#000000"
        android:pathData="{g.poly_path(launcher.mountain)}" />
    <path android:fillColor="#000000"
        android:pathData="{g.circle_path(*launcher.sun)}" />
</vector>
'''

    # 앱 내 로고: 런처 마스크가 없으므로 배경을 직접 둥근 사각형으로 그리고 도형을 크게 채운다.
    logo_xml = HDR + f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
{sky_gradient(g.rrect_path(0, 0, 108, 108, 24))}
{album_paths(logo)}
</vector>
'''

    for name, body in (("ic_launcher_background", bg),
                       ("ic_launcher_foreground", fg),
                       ("ic_launcher_monochrome", mono),
                       ("ic_app_logo", logo_xml)):
        io.open(f"{RES}/drawable/{name}.xml", "w", encoding="utf-8").write(body)
    print("벡터 4개 생성")


SS = 8  # 수퍼샘플링


def _draw_album(d, a, k, ox, oy):
    def box(rect):
        x0, y0, x1, y1 = rect
        return [x0 * k + ox, y0 * k + oy, x1 * k + ox, y1 * k + oy]

    d.rounded_rectangle(box(a.sheet), radius=a.sheet_r * k, fill=g.SHEET)
    d.rounded_rectangle(box(a.card), radius=a.card_r * k, fill=g.CARD)

    # 앨범 등: 왼쪽만 둥글게 -> 둥근 사각형 + 오른쪽 절반 메우기
    sx0, sy0, sx1, sy1 = a.spine
    d.rounded_rectangle(box(a.spine), radius=a.card_r * k, fill=g.SPINE)
    d.rectangle(box(((sx0 + sx1) / 2, sy0, sx1, sy1)), fill=g.SPINE)

    d.rectangle(box(a.window), fill=g.WINDOW)

    cx, cy, rr = a.sun
    d.ellipse(box((cx - rr, cy - rr, cx + rr, cy + rr)), fill=g.SUN)
    d.polygon([(x * k + ox, y * k + oy) for x, y in a.mountain], fill=g.MOUNTAIN)


def render(size, round_mask):
    W = size * SS
    img = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    top = tuple(int(g.BG_TOP[1 + i * 2:3 + i * 2], 16) for i in range(3))
    bot = tuple(int(g.BG_BOTTOM[1 + i * 2:3 + i * 2], 16) for i in range(3))
    for y in range(W):
        t = y / (W - 1)
        d.line([(0, y), (W, y)],
               fill=tuple(round(top[i] * (1 - t) + bot[i] * t) for i in range(3)) + (255,))

    # 적응형 아이콘 안전 영역(가운데 72)이 레거시 캔버스를 채우도록 확대
    k = W / 72.0
    _draw_album(d, g.launcher_art(), k, -18 * k, -18 * k)

    # 런처가 마스크를 씌우지 않으므로 아이콘 스스로 모양을 갖는다
    mask = Image.new("L", (W, W), 0)
    md = ImageDraw.Draw(mask)
    if round_mask:
        md.ellipse([0, 0, W - 1, W - 1], fill=255)
    else:
        md.rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.22), fill=255)
    img.putalpha(mask)
    return img.resize((size, size), Image.LANCZOS)


def write_bitmaps():
    for dens, size in {"mdpi": 48, "hdpi": 72, "xhdpi": 96,
                       "xxhdpi": 144, "xxxhdpi": 192}.items():
        for name, is_round in (("ic_launcher", False), ("ic_launcher_round", True)):
            render(size, is_round).save(f"{RES}/mipmap-{dens}/{name}.webp",
                                        "WEBP", lossless=True, method=6)
    print("레거시 비트맵 10개 생성")


if __name__ == "__main__":
    write_vectors()
    write_bitmaps()
    # 눈으로 확인하려면 아래 주석을 풀어 큰 미리보기를 만든다(저장소에는 남기지 않는다).
    # render(512, False).save("/tmp/icon_square.png")
    # render(512, True).save("/tmp/icon_round.png")
