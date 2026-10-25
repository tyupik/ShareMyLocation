"""Точечный блюр чувствительных данных (email, телефоны) на скриншотах.

Координаты взяты из UI Automator dump (bounds текстовых узлов) с запасом 10px.
"""

from PIL import Image, ImageFilter

PADDING = 10
BLUR_RADIUS = 18

# файл -> список прямоугольников (left, top, right, bottom)
REGIONS = {
    "docs/screenshots/02-friends.png": [
        (264, 683, 502, 738),   # телефон друга 89151344343
    ],
    "docs/screenshots/04-profile.png": [
        (154, 602, 520, 654),   # email potypkin@mail.ru
        (154, 701, 446, 753),   # телефон 89063553927
    ],
}

for path, rects in REGIONS.items():
    img = Image.open(path).convert("RGB")
    for (l, t, r, b) in rects:
        box = (max(l - PADDING, 0), max(t - PADDING, 0),
               min(r + PADDING, img.width), min(b + PADDING, img.height))
        region = img.crop(box)
        blurred = region.filter(ImageFilter.GaussianBlur(BLUR_RADIUS))
        img.paste(blurred, box)
    img.save(path)
    print(f"blurred: {path}")
