from PIL import Image, ImageDraw
import os

output_dir = r'p:\my-new-project\miniprogram\images'
os.makedirs(output_dir, exist_ok=True)

# 创建深色主题TabBar图标
def create_tab_icon(name, draw_func):
    # 正常状态 - 半透明金色
    size = 81
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    draw_func(draw, size, (200, 168, 122, 128))  # 半透明金色
    img.save(os.path.join(output_dir, f'tab-{name}.png'))
    
    # 选中状态 - 实色金色
    img_active = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw_active = ImageDraw.Draw(img_active)
    draw_func(draw_active, size, (200, 168, 122, 255))  # 实色金色
    img_active.save(os.path.join(output_dir, f'tab-{name}-active.png'))
    
    print(f'tab-{name}.png created')

# 首页图标
def draw_home(draw, size, color):
    center = size // 2
    # 房子轮廓
    draw.polygon([
        (center, 10),
        (10, center - 5),
        (center - 20, center - 5),
        (center - 20, size - 10),
        (center + 20, size - 10),
        (center + 20, center - 5),
        (size - 10, center - 5)
    ], outline=color, width=3)
    # 门
    draw.rectangle([center - 8, size - 25, center + 8, size - 10], outline=color, width=2)

# 菜单图标
def draw_menu(draw, size, color):
    margin = 15
    grid_size = (size - margin * 2 - 10) // 2
    for row in range(2):
        for col in range(2):
            x = margin + col * (grid_size + 10)
            y = margin + row * (grid_size + 10)
            draw.rectangle([x, y, x + grid_size, y + grid_size], outline=color, width=3)

# 购物车图标
def draw_cart(draw, size, color):
    # 购物车主体
    draw.polygon([
        (10, 20),
        (25, 20),
        (30, 50),
        (70, 50),
        (75, 20),
        (size - 10, 20)
    ], outline=color, width=3)
    # 轮子
    draw.ellipse([25, size - 20, 35, size - 10], outline=color, width=2)
    draw.ellipse([50, size - 20, 60, size - 10], outline=color, width=2)

# 个人中心图标
def draw_profile(draw, size, color):
    center = size // 2
    # 头部
    draw.ellipse([center - 15, 10, center + 15, 40], outline=color, width=3)
    # 身体
    draw.arc([10, 35, size - 10, size - 5], start=200, end=340, fill=color, width=3)

create_tab_icon('home', draw_home)
create_tab_icon('menu', draw_menu)
create_tab_icon('cart', draw_cart)
create_tab_icon('profile', draw_profile)

print('All tab icons created!')