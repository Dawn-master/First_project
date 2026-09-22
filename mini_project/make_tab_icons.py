from PIL import Image, ImageDraw
import os

base = r"D:\WeChatProjects\多传感器环境监测系统\images\tabs"
os.makedirs(base, exist_ok=True)

def make(name, draw_fn, color):
    img = Image.new("RGBA", (81, 81), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    draw_fn(d, color)
    img.save(os.path.join(base, name + ".png"))

def home(d, c):
    d.polygon([(40, 12), (12, 40), (24, 40), (24, 68), (56, 68), (56, 40), (68, 40)], outline=c)
    d.rectangle([32, 48, 48, 68], outline=c)

def alert(d, c):
    d.ellipse([18, 30, 62, 74], outline=c, width=3)
    d.polygon([(40, 14), (28, 34), (52, 34)], outline=c)
    d.line([40, 48, 40, 62], fill=c, width=4)
    d.ellipse([37, 64, 43, 70], fill=c)

def chart(d, c):
    d.rectangle([16, 16, 64, 64], outline=c, width=3)
    d.line([24, 54, 34, 40], fill=c, width=4)
    d.line([34, 40, 44, 48], fill=c, width=4)
    d.line([44, 48, 56, 28], fill=c, width=4)

def device(d, c):
    d.rounded_rectangle([22, 18, 58, 55], radius=6, outline=c, width=3)
    d.line([32, 62, 48, 62], fill=c, width=3)
    d.line([40, 55, 40, 62], fill=c, width=3)
    d.ellipse([36, 30, 44, 38], outline=c, width=2)

gray = (122, 139, 134, 255)
teal = (31, 122, 110, 255)
for n, f in [("home", home), ("alert", alert), ("stats", chart), ("device", device)]:
    make(n + "-nor", f, gray)
    make(n + "-sel", f, teal)
print("icons ok:", os.listdir(base))
