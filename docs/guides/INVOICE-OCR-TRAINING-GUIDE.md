# 发票OCR专用模型训练指南

## 一、训练需求分析

### 目标
训练一个专门识别发票的OCR模型，提高发票识别准确率

### 技术路线
```
PaddleOCR训练 → 导出模型 → 转换ONNX → RapidOCR使用
```

### 模型选择
- **检测模型**: PP-OCRv4 det (文字检测)
- **识别模型**: PP-OCRv4 rec (文字识别)
- **方向分类**: 可选 (文字方向)

## 二、数据集准备

### 2.1 数据收集
需要收集以下类型的发票样本：
- 增值税普通发票
- 增值税专用发票
- 增值税电子发票
- 不同地区、不同行业的发票

### 2.2 数据标注

#### 检测模型标注
使用LabelImg或PPOCRLabel标注文字区域：
```bash
pip install PPOCRLabel
PPOCRLabel --lang ch
```

标注格式 (Label.txt):
```
图像路径\t[{"transcription": "文字内容", "points": [[x1,y1],[x2,y2],[x3,y3],[x4,y4]]}, ...]
```

#### 识别模型标注
使用文本行标注：
```
图像路径\t文字内容
```

### 2.3 数据集结构
```
invoice_dataset/
├── det_train/
│   ├── images/
│   │   ├── invoice_001.jpg
│   │   ├── invoice_002.jpg
│   │   └── ...
│   └── Label.txt
├── det_val/
│   ├── images/
│   └── Label.txt
├── rec_train/
│   ├── images/
│   │   ├── word_001.jpg
│   │   ├── word_002.jpg
│   │   └── ...
│   └── gt.txt
└── rec_val/
    ├── images/
    └── gt.txt
```

### 2.4 字典文件
创建发票专用字典 `invoice_dict.txt`:
```
0
1
2
3
4
5
6
7
8
9
a
b
c
...
发
票
代
码
号
...
```

## 三、训练环境配置

### 3.1 安装PaddleOCR
```bash
# 在项目虚拟环境中
p:\my-new-project\venv-paddleocr\Scripts\activate
pip install paddleocr
pip install paddlepaddle-gpu  # 如有GPU
```

### 3.2 克隆PaddleOCR仓库
```bash
cd p:\my-new-project
git clone https://github.com/PaddlePaddle/PaddleOCR.git
```

### 3.3 安装依赖
```bash
cd PaddleOCR
pip install -r requirements.txt
```

## 四、模型训练

### 4.1 检测模型训练

#### 配置文件
创建 `configs/det/ch_PP-OCRv4/ch_PP-OCRv4_det_invoice.yml`:
```yaml
Global:
  debug: false
  use_gpu: true
  epoch_num: 500
  log_smooth_window: 20
  print_batch_step: 10
  save_model_dir: ./output/det_invoice
  save_epoch_step: 10
  eval_batch_step: [0, 500]
  cal_metric_during_train: false
  pretrained_model: ./pretrain_models/ch_PP-OCRv4_det_train/best_accuracy
  checkpoints:
  save_inference_dir:
  use_visualdl: false
  infer_img: doc/imgs_en/img_10.jpg
  save_res_path: ./output/det_invoice/predicts_db.txt

Architecture:
  model_type: det
  algorithm: DB
  Transform:
  Backbone:
    name: PPLCNetV3
    scale: 0.75
    det: true
  Neck:
    name: RSEFPN
    out_channels: 96
    shortcut: True
  Head:
    name: DBHead
    k: 50
    out_channels: 96

Loss:
  name: DBLoss
  balance_loss: true
  main_loss_type: DiceLoss
  alpha: 5
  beta: 10
  ohem_ratio: 3

Optimizer:
  name: Adam
  beta1: 0.9
  beta2: 0.999
  lr:
    name: Cosine
    learning_rate: 0.001
    warmup_epoch: 2
  regularizer:
    name: L2
    factor: 5.0e-05

PostProcess:
  name: DBPostProcess
  thresh: 0.3
  box_thresh: 0.6
  max_candidates: 1000
  unclip_ratio: 1.5

Metric:
  name: DetMetric
  main_indicator: hmean

Train:
  dataset:
    name: SimpleDataSet
    data_dir: ./invoice_dataset/det_train
    label_file_list:
      - ./invoice_dataset/det_train/Label.txt
    ratio_list: [1.0]
    transforms:
      - DecodeImage:
          img_mode: BGR
          channel_first: false
      - DetLabelEncode: null
      - CopyPaste: null
      - IaaAugment:
          augmenter_args:
            - type: Fliplr
              args:
                p: 0.5
            - type: Affine
              args:
                rotate:
                  - -10
                  - 10
            - type: Resize
              args:
                size:
                  - 0.5
                  - 3
      - EastRandomCropData:
          size:
            - 960
            - 960
          max_tries: 50
          keep_ratio: true
      - MakeBorderMap:
          shrink_ratio: 0.4
          thresh_min: 0.3
          thresh_max: 0.7
      - MakeShrinkMap:
          shrink_ratio: 0.4
          min_text_size: 8
      - NormalizeImage:
          scale: 1./255.
          mean:
            - 0.485
            - 0.456
            - 0.406
          std:
            - 0.229
            - 0.224
            - 0.225
          order: hwc
      - ToCHWImage: null
      - KeepKeys:
          keep_keys:
            - image
            - threshold_map
            - threshold_mask
            - shrink_map
            - shrink_mask
            - text
  loader:
    shuffle: true
    drop_last: false
    batch_size_per_card: 8
    num_workers: 4

Eval:
  dataset:
    name: SimpleDataSet
    data_dir: ./invoice_dataset/det_val
    label_file_list:
      - ./invoice_dataset/det_val/Label.txt
    ratio_list: [1.0]
    transforms:
      - DecodeImage:
          img_mode: BGR
          channel_first: false
      - DetLabelEncode: null
      - DetResizeForTest:
          limit_type: max
          limit_size: 960
      - NormalizeImage:
          scale: 1./255.
          mean:
            - 0.485
            - 0.456
            - 0.406
          std:
            - 0.229
            - 0.224
            - 0.225
          order: hwc
      - ToCHWImage: null
      - KeepKeys:
          keep_keys:
            - image
            - shape
            - text
  loader:
    shuffle: false
    drop_last: false
    batch_size_per_card: 1
    num_workers: 2
```

#### 开始训练
```bash
python tools/train.py -c configs/det/ch_PP-OCRv4/ch_PP-OCRv4_det_invoice.yml
```

### 4.2 识别模型训练

#### 配置文件
创建 `configs/rec/PP-OCRv4/ch_PP-OCRv4_rec_invoice.yml`:
```yaml
Global:
  debug: false
  use_gpu: true
  epoch_num: 100
  log_smooth_window: 20
  print_batch_step: 10
  save_model_dir: ./output/rec_invoice
  save_epoch_step: 5
  eval_batch_step: [0, 500]
  cal_metric_during_train: true
  pretrained_model: ./pretrain_models/ch_PP-OCRv4_rec_train/best_accuracy
  checkpoints:
  save_inference_dir:
  use_visualdl: false
  infer_img: doc/imgs_words/ch/word_1.jpg
  character_dict_path: ./invoice_dataset/invoice_dict.txt
  max_text_length: 25
  infer_mode: false
  use_space_char: true
  save_res_path: ./output/rec_invoice/predicts_ch.txt

Architecture:
  model_type: rec
  algorithm: SVTR_LCNet
  Transform:
  Backbone:
    name: PPLCNetV3
    scale: 0.95
  Head:
    name: CTCHead
    fc_decay: 1.0e-05

Loss:
  name: CTCLoss

Optimizer:
  name: Adam
  beta1: 0.9
  beta2: 0.999
  lr:
    name: Cosine
    learning_rate: 0.001
    warmup_epoch: 5
  regularizer:
    name: L2
    factor: 1.0e-05

PostProcess:
  name: CTCLabelDecode
  character_dict_path: ./invoice_dataset/invoice_dict.txt

Metric:
  name: RecMetric
  main_indicator: acc
  is_filter: true

Train:
  dataset:
    name: SimpleDataSet
    data_dir: ./invoice_dataset/rec_train
    label_file_list:
      - ./invoice_dataset/rec_train/gt.txt
    ratio_list: [1.0]
    transforms:
      - DecodeImage:
          img_mode: BGR
          channel_first: false
      - RecConAug:
          prob: 0.5
          ext_data_num: 2
          image_shape:
            - 48
            - 320
            - 3
          max_text_length: 25
      - RecAug: null
      - CTCLabelEncode: null
      - RecResizeImg:
          image_shape:
            - 3
            - 48
            - 320
      - KeepKeys:
          keep_keys:
            - image
            - label
            - length
  loader:
    shuffle: true
    batch_size_per_card: 128
    drop_last: true
    num_workers: 8

Eval:
  dataset:
    name: SimpleDataSet
    data_dir: ./invoice_dataset/rec_val
    label_file_list:
      - ./invoice_dataset/rec_val/gt.txt
    ratio_list: [1.0]
    transforms:
      - DecodeImage:
          img_mode: BGR
          channel_first: false
      - CTCLabelEncode: null
      - RecResizeImg:
          image_shape:
            - 3
            - 48
            - 320
      - KeepKeys:
          keep_keys:
            - image
            - label
            - length
  loader:
    shuffle: false
    drop_last: false
    batch_size_per_card: 128
    num_workers: 4
```

#### 开始训练
```bash
python tools/train.py -c configs/rec/PP-OCRv4/ch_PP-OCRv4_rec_invoice.yml
```

## 五、模型导出与转换

### 5.1 导出推理模型
```bash
# 导出检测模型
python tools/export_model.py -c configs/det/ch_PP-OCRv4/ch_PP-OCRv4_det_invoice.yml \
    -o Global.pretrained_model=./output/det_invoice/best_accuracy \
    Global.save_inference_dir=./output/det_invoice/inference

# 导出识别模型
python tools/export_model.py -c configs/rec/PP-OCRv4/ch_PP-OCRv4_rec_invoice.yml \
    -o Global.pretrained_model=./output/rec_invoice/best_accuracy \
    Global.save_inference_dir=./output/rec_invoice/inference
```

### 5.2 转换为ONNX格式
```bash
# 安装转换工具
pip install paddle2onnx

# 转换检测模型
paddle2onnx --model_dir ./output/det_invoice/inference \
    --model_filename inference.pdmodel \
    --params_filename inference.pdiparams \
    --save_file ./output/det_invoice/invoice_det.onnx \
    --opset_version 14

# 转换识别模型
paddle2onnx --model_dir ./output/rec_invoice/inference \
    --model_filename inference.pdmodel \
    --params_filename inference.pdiparams \
    --save_file ./output/rec_invoice/invoice_rec.onnx \
    --opset_version 14
```

## 六、集成到RapidOCR

### 6.1 复制模型文件
```bash
# 复制到RapidOCR模型目录
cp ./output/det_invoice/invoice_det.onnx p:/my-new-project/ocr-models/
cp ./output/rec_invoice/invoice_rec.onnx p:/my-new-project/ocr-models/
```

### 6.2 修改RapidOCR服务配置
更新 `rapidocr_invoice_service_v2.py`:
```python
from rapidocr_onnxruntime import RapidOCR

# 使用自定义模型
ocr_engine = RapidOCR(
    det_model_path="p:/my-new-project/ocr-models/invoice_det.onnx",
    rec_model_path="p:/my-new-project/ocr-models/invoice_rec.onnx",
    # 其他参数...
)
```

## 七、训练数据建议

### 7.1 数据量建议
- 检测模型: 至少1000张发票图片
- 识别模型: 至少10000个文字行样本

### 7.2 数据增强
- 旋转: -10° ~ 10°
- 缩放: 0.5 ~ 3倍
- 噪声: 高斯噪声
- 模糊: 轻微模糊
- 对比度: 随机调整

### 7.3 注意事项
1. 确保数据多样性（不同发票类型、不同地区）
2. 标注质量要高，错误标注会影响模型效果
3. 定期验证模型效果，及时调整训练参数
4. 保存训练日志，便于分析问题

## 八、训练资源需求

### 8.1 硬件要求
- GPU: NVIDIA GPU (推荐RTX 3060或更高)
- 显存: 至少8GB
- 内存: 至少16GB
- 存储: 至少50GB

### 8.2 训练时间估算
- 检测模型: 约10-20小时 (取决于数据量)
- 识别模型: 约20-40小时 (取决于数据量)

## 九、后续优化

### 9.1 模型量化
```bash
# FP16量化
python tools/post_quantization.py --model_dir ./output/det_invoice/inference \
    --save_model_dir ./output/det_invoice/quant \
    --quant_type "fp16"
```

### 9.2 模型剪枝
```bash
# 敏感度分析
python tools/sensitivity_analysis.py -c configs/det/ch_PP-OCRv4/ch_PP-OCRv4_det_invoice.yml

# 剪枝
python tools/prune.py -c configs/det/ch_PP-OCRv4/ch_PP-OCRv4_det_invoice.yml
```

### 9.3 知识蒸馏
```bash
# 使用大模型蒸馏小模型
python tools/distill.py -c configs/distill/ch_PP-OCRv4_distill.yml
```
