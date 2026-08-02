from PIL import Image, ImageDraw
import os

output_dir = r'p:\my-new-project\miniprogram\images\decorations'
os.makedirs(output_dir, exist_ok=True)

# 创建深色中式边框装饰
def create_chinese_border():
    width, height = 400, 400
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 金色
    gold_color = (200, 168, 122, 255)
    gold_light = (218, 190, 150, 255)
    
    # 外框
    margin = 20
    draw.rectangle([margin, margin, width-margin, height-margin], outline=gold_color, width=3)
    
    # 内框
    inner_margin = 35
    draw.rectangle([inner_margin, inner_margin, width-inner_margin, height-inner_margin], outline=gold_light, width=1)
    
    # 四角装饰
    corner_size = 30
    corners = [
        (margin, margin),
        (width-margin-corner_size, margin),
        (margin, height-margin-corner_size),
        (width-margin-corner_size, height-margin-corner_size)
    ]
    
    for cx, cy in corners:
        # 角落L形装饰
        draw.line([cx, cy, cx + corner_size, cy], fill=gold_color, width=4)
        draw.line([cx, cy, cx, cy + corner_size], fill=gold_color, width=4)
    
    img.save(os.path.join(output_dir, 'chinese-border.png'))
    print('chinese-border.png created')

# 创建中式云纹装饰
def create_cloud_pattern():
    width, height = 200, 100
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gold_color = (200, 168, 122, 200)
    
    # 绘制云纹
    import math
    for i in range(3):
        cx = 30 + i * 60
        cy = 50
        for angle in range(0, 180, 5):
            r = 20
            x = cx + int(r * math.cos(math.radians(angle)))
            y = cy + int(r * 0.5 * math.sin(math.radians(angle)))
            if angle == 0:
                prev_x, prev_y = x, y
            else:
                draw.line([prev_x, prev_y, x, y], fill=gold_color, width=2)
                prev_x, prev_y = x, y
    
    img.save(os.path.join(output_dir, 'cloud-pattern.png'))
    print('cloud-pattern.png created')

# 创建中式回纹边框
def create_meander_border():
    width, height = 750, 100
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gold_color = (200, 168, 122, 255)
    
    # 绘制连续回纹
    unit_size = 50
    for i in range(0, width, unit_size):
        # 回纹单元
        x, y = i, 25
        s = 40
        draw.rectangle([x+5, y, x+s, y+s], outline=gold_color, width=2)
        inner = 10
        draw.rectangle([x+5+inner, y+inner, x+s-inner, y+s-inner], outline=gold_color, width=1)
    
    img.save(os.path.join(output_dir, 'meander-border.png'))
    print('meander-border.png created')

# 创建深色渐变背景
def create_dark_bg():
    width, height = 750, 1334
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 从深棕到黑色渐变
    for y in range(height):
        ratio = y / height
        r = int(45 - ratio * 25)
        g = int(35 - ratio * 20)
        b = int(30 - ratio * 15)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))
    
    img.save(os.path.join(output_dir, 'dark-bg.png'))
    print('dark-bg.png created')

# 创建金色装饰线
def create_gold_line():
    width, height = 200, 4
    img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gold_color = (200, 168, 122, 255)
    draw.line([(0, 2), (width, 2)], fill=gold_color, width=2)
    
    img.save(os.path.join(output_dir, 'gold-line.png'))
    print('gold-line.png created')

# 创建中式圆形装饰
def create_circle_decoration():
    size = 120
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    gold_color = (200, 168, 122, 255)
    gold_light = (218, 190, 150, 180)
    
    center = size // 2
    
    # 外圆
    draw.ellipse([10, 10, size-10, size-10], outline=gold_color, width=2)
    
    # 内圆
    draw.ellipse([25, 25, size-25, size-25], outline=gold_light, width=1)
    
    # 中心点
    draw.ellipse([center-3, center-3, center+3, center+3], fill=gold_color)
    
    img.save(os.path.join(output_dir, 'circle-deco.png'))
    print('circle-deco.png created')

# 创建所有装饰
create_chinese_border()
create_cloud_pattern()
create_meander_border()
create_dark_bg()
create_gold_line()
create_circle_decoration()

print('All decorations created successfully!')