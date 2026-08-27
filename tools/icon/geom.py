"""런처 아이콘/앱 내 로고의 도형 정의.

좌표계는 적응형 아이콘과 같은 108x108.

같은 밑그림을 두 가지 배율로 쓴다.
 - 런처 아이콘: 원형 마스크에 잘리지 않도록 줄인다([LAUNCHER_SCALE]).
   런처가 마스크를 씌우면 가운데 지름 66(반지름 33)만 확실히 보이는데,
   밑그림 그대로는 표지 모서리가 반지름 41 까지 나가서 잘린다.
 - 앱 내 로고: 마스크가 없으므로 오히려 키워서 꽉 채운다([LOGO_SCALE]).
"""

# 하늘색 배경
BG_TOP = "#BFE6F9"
BG_BOTTOM = "#6FB6DC"

# 브라운 톤
SHEET = "#D9A97F"   # 뒤에 겹쳐진 사진
CARD = "#6E4326"    # 앨범 표지
SPINE = "#54331E"   # 앨범 등
WINDOW = "#FAEFE0"  # 표지 안쪽 사진
MOUNTAIN = "#9C6239"
SUN = "#E5A44F"

CENTER = (54.0, 54.0)
LAUNCHER_SCALE = 0.78
LOGO_SCALE = 1.35

# ── 밑그림 (배율 1.0 기준) ──────────────────────────────
_SHEET_RECT = (41, 27, 84, 57)
_SHEET_R = 3.5
_CARD_RECT = (24, 38, 80, 84)
_CARD_R = 6.0
_SPINE_RECT = (24, 38, 33, 84)
_WINDOW_RECT = (38, 46, 74, 77)
_SUN_C = (47.5, 55.5, 4.3)
_MOUNTAIN_PTS = [(38, 77), (51, 57.5), (59.5, 68), (65.5, 60.5), (74, 77)]


class Art:
    """[scale] 배로 확대/축소한 도형 묶음."""

    def __init__(self, scale, center=CENTER):
        self.scale = scale
        self.cx, self.cy = center
        self.sheet = self._rect(_SHEET_RECT)
        self.sheet_r = self._len(_SHEET_R)
        self.card = self._rect(_CARD_RECT)
        self.card_r = self._len(_CARD_R)
        self.spine = self._rect(_SPINE_RECT)
        self.window = self._rect(_WINDOW_RECT)
        self.sun = (self._x(_SUN_C[0]), self._y(_SUN_C[1]), self._len(_SUN_C[2]))
        self.mountain = [(self._x(x), self._y(y)) for x, y in _MOUNTAIN_PTS]

    def _x(self, x):
        return round(self.cx + (x - self.cx) * self.scale, 2)

    def _y(self, y):
        return round(self.cy + (y - self.cy) * self.scale, 2)

    def _len(self, v):
        return round(v * self.scale, 2)

    def _rect(self, rect):
        x0, y0, x1, y1 = rect
        return (self._x(x0), self._y(y0), self._x(x1), self._y(y1))

    def spine_path(self):
        """앨범 등: 표지 왼쪽 모서리만 둥글고 오른쪽은 직선."""
        x0, y0, x1, y1 = self.spine
        r = self.card_r
        return (
            f"M{x1},{y0} L{x0 + r},{y0} A{r},{r} 0 0 0 {x0},{y0 + r} "
            f"L{x0},{y1 - r} A{r},{r} 0 0 0 {x0 + r},{y1} L{x1},{y1} Z"
        )


def launcher_art():
    return Art(LAUNCHER_SCALE)


def logo_art():
    return Art(LOGO_SCALE)


# ── path 문자열 생성 ────────────────────────────────────
def rrect_path(x0, y0, x1, y1, r):
    return (
        f"M{x0 + r},{y0} L{x1 - r},{y0} A{r},{r} 0 0 1 {x1},{y0 + r} "
        f"L{x1},{y1 - r} A{r},{r} 0 0 1 {x1 - r},{y1} "
        f"L{x0 + r},{y1} A{r},{r} 0 0 1 {x0},{y1 - r} "
        f"L{x0},{y0 + r} A{r},{r} 0 0 1 {x0 + r},{y0} Z"
    )


def rect_path(x0, y0, x1, y1):
    return f"M{x0},{y0} L{x1},{y0} L{x1},{y1} L{x0},{y1} Z"


def circle_path(cx, cy, r):
    return f"M{cx - r},{cy} A{r},{r} 0 1 0 {cx + r},{cy} A{r},{r} 0 1 0 {cx - r},{cy} Z"


def poly_path(pts):
    head = f"M{pts[0][0]},{pts[0][1]}"
    rest = " ".join(f"L{x},{y}" for x, y in pts[1:])
    return f"{head} {rest} Z"
