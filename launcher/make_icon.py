from PIL import Image, ImageDraw, ImageFont
import struct, zlib, os

SIZE = 256
img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
d = ImageDraw.Draw(img)

# rounded violet square background
def rounded(draw, xy, r, fill):
    x0, y0, x1, y1 = xy
    draw.pieslice([x0, y0, x0 + 2 * r, y0 + 2 * r], 180, 270, fill=fill)
    draw.pieslice([x1 - 2 * r, y0, x1, y0 + 2 * r], 270, 360, fill=fill)
    draw.pieslice([x1 - 2 * r, y1 - 2 * r, x1, y1], 0, 90, fill=fill)
    draw.pieslice([x0, y1 - 2 * r, x0 + 2 * r, y1], 90, 180, fill=fill)
    draw.rectangle([x0 + r, y0, x1 - r, y1], fill=fill)
    draw.rectangle([x0, y0 + r, x1, y1 - r], fill=fill)

rounded(d, [16, 16, SIZE - 16, SIZE - 16], 48, (124, 58, 237, 255))
rounded(d, [16, 16, SIZE - 16, SIZE - 16], 48, None)
# subtle inner highlight
rounded(d, [30, 30, SIZE - 30, SIZE - 30], 40, (145, 85, 245, 120))

# "EX" text
try:
    f = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 150)
except Exception:
    f = ImageFont.load_default()
text = "EX"
bbox = d.textbbox((0, 0), text, font=f)
tw, th = bbox[2] - bbox[0], bbox[3] - bbox[1]
d.text(((SIZE - tw) / 2 - bbox[0], (SIZE - th) / 2 - bbox[1]), text, font=f, fill=(255, 255, 255, 255))

# Save as multi-size ICO
out = "C:/Users/11/AppData/Local/Temp/opencode/Clan-Exzodus/launcher/icon.ico"
img.save(out, sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
print("icon saved", os.path.getsize(out), "bytes")
