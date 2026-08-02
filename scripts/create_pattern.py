from PIL import Image, ImageDraw
import os

output_dir = r'p:\my-new-project\miniprogram\images'
os.makedirs(output_dir, exist_ok=True)

width, height = 200, 200
img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

pattern_color = (255, 255, 255, 25)

def draw_meander_pattern(draw, x, y, size, color):
    s = size
    line_width = 2
    draw.rectangle([x, y, x+s, y+s], outline=color, width=line_width)
    inner_margin = s // 4
    draw.rectangle([x+inner_margin, y+inner_margin, x+s-inner_margin, y+s-inner_margin], outline=color, width=line_width)
    mid = s // 2
    draw.line([x+inner_margin, y+mid, x+mid, y+mid], fill=color, width=line_width)
    draw.line([x+mid, y+mid, x+mid, y+s-inner_margin], fill=color, width=line_width)

def draw_cloud_pattern(draw, cx, cy, radius, color):
    for i in range(3):
        offset = i * radius // 2
        draw.arc([cx-radius+offset, cy-radius, cx+radius+offset, cy+radius], 
                 start=0, end=180, fill=color, width=2)

pattern_size = 40
for row in range(0, height, pattern_size):
    for col in range(0, width, pattern_size):
        if (row // pattern_size + col // pattern_size) % 2 == 0:
            draw_meander_pattern(draw, col, row, pattern_size-4, pattern_color)
        else:
            draw_cloud_pattern(draw, col + pattern_size//2, row + pattern_size//2, 
                             pattern_size//4, pattern_color)

img.save(os.path.join(output_dir, 'pattern.png'))
print('pattern.png created')
