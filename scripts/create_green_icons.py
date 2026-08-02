from PIL import Image, ImageDraw
import os

output_dir = r'p:\my-new-project\miniprogram\images'
os.makedirs(output_dir, exist_ok=True)

def create_tab_icon(name, draw_func):
    size = 81
    # 正常状态 - 灰色
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_func(draw, size, (189, 189, 189, 255))
    img.save(os.path.join(output_dir, f'tab-{name}.png'))
    
    # 选中状态 - 绿色
    img_active = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw_active = ImageDraw.Draw(img_active)
    draw_func(draw_active, size, (76, 175, 80, 255))
    img_active.save(os.path.join(output_dir, f'tab-{name}-active.png'))
    
    print(f'tab-{name}.png created')

# 首页图标 - 房子
def draw_home(draw, size, color):
    center = size // 2
    # 房子轮廓
    points = [
        (center, 12),
        (14, center - 8),
        (center - 18, center - 8),
        (center - 18, size - 14),
        (center + 18, size - 14),
        (center + 18, center - 8),
        (size - 14, center - 8)
    ]
    draw.polygon(points, outline=color, width=4)
    # 门
    draw.rectangle([center - 10, size - 28, center + 10, size - 14], outline=color, width=3)

# 菜单图标 - 网格
def draw_menu(draw, size, color):
    margin = 15
    grid_size = (size - margin * 2 - 10) // 2
    for row in range(2):
        for col in range(2):
            x = margin + col * (grid_size + 10)
            y = margin + row * (grid_size + 10)
            draw.rectangle([x, y, x + grid_size, y + grid_size], outline=color, width=4)

# 购物车图标
def draw_cart(draw, size, color):
    # 购物车主体
    draw.polygon([
        (14, 22),
        (26, 22),
        (30, 50),
        (55, 50),
        (60, 22),
        (size - 14, 22)
    ], outline=color, width=4)
    # 轮子
    draw.ellipse([26, size - 22, 36, size - 12], outline=color, width=3)
    draw.ellipse([48, size - 22, 58, size - 12], outline=color, width=3)

# 个人中心图标
def draw_profile(draw, size, color):
    center = size // 2
    # 头部
    draw.ellipse([center - 16, 12, center + 16, 44], outline=color, width=4)
    # 身体
    draw.arc([14, 38, size - 14, size - 8], start=200, end=340, fill=color, width=4)

create_tab_icon('home', draw_home)
create_tab_icon('menu', draw_menu)
create_tab_icon('cart', draw_cart)
create_tab_icon('profile', draw_profile)

print('All tab icons created!')