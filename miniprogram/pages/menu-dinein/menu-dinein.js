const app = getApp()

Page({
  data: {
    statusBarHeight: 0,
    headerHeight: 0,
    tableNo: 'A01',
    orderType: 'dinein',
    currentCategory: 'hot',
    currentCategoryName: '热销推荐',
    categories: [
      { id: 'hot', name: '热销推荐', count: 8 },
      { id: 'rice', name: '米饭套餐', count: 12 },
      { id: 'noodle', name: '面食点心', count: 6 },
      { id: 'snack', name: '小食配菜', count: 10 },
      { id: 'drink', name: '饮品甜点', count: 8 }
    ],
    products: [
      {
        id: 'p1',
        name: '红烧肉套餐',
        desc: '精选五花肉，慢炖入味，搭配新鲜时蔬',
        price: 32,
        originalPrice: 38,
        image: '',
        isNew: false,
        isHot: true,
        quantity: 0
      },
      {
        id: 'p2',
        name: '糖醋里脊套餐',
        desc: '外酥里嫩，酸甜可口',
        price: 28,
        originalPrice: null,
        image: '',
        isNew: true,
        isHot: false,
        quantity: 0
      },
      {
        id: 'p3',
        name: '宫保鸡丁套餐',
        desc: '经典川菜，麻辣鲜香',
        price: 26,
        originalPrice: null,
        image: '',
        isNew: false,
        isHot: true,
        quantity: 0
      },
      {
        id: 'p4',
        name: '清蒸鲈鱼套餐',
        desc: '新鲜鲈鱼，清淡鲜美',
        price: 48,
        originalPrice: 58,
        image: '',
        isNew: true,
        isHot: false,
        quantity: 0
      }
    ],
    cartCount: 0,
    cartTotal: 0
  },

  onLoad(options) {
    const systemInfo = wx.getSystemInfoSync()
    app.setCurrentOrderType('dinein')
    this.setData({
      statusBarHeight: systemInfo.statusBarHeight,
      headerHeight: systemInfo.statusBarHeight * 2 + 72,
      tableNo: options.tableNo || 'A01'
    })
  },

  onShow() {
    this.updateCartInfo()
    this.loadProducts(this.data.currentCategory)
  },

  selectCategory(e) {
    const id = e.currentTarget.dataset.id
    const category = this.data.categories.find(c => c.id === id)
    this.setData({
      currentCategory: id,
      currentCategoryName: category ? category.name : ''
    })
    this.loadProducts(id)
  },

  loadProducts(categoryId) {
    const allProducts = {
      hot: [
        { id: 'p1', name: '红烧肉套餐', desc: '精选五花肉，慢炖入味，搭配新鲜时蔬', price: 32, originalPrice: 38, image: '', isNew: false, isHot: true, quantity: 0 },
        { id: 'p2', name: '糖醋里脊套餐', desc: '外酥里嫩，酸甜可口', price: 28, originalPrice: null, image: '', isNew: true, isHot: false, quantity: 0 },
        { id: 'p3', name: '宫保鸡丁套餐', desc: '经典川菜，麻辣鲜香', price: 26, originalPrice: null, image: '', isNew: false, isHot: true, quantity: 0 },
        { id: 'p4', name: '清蒸鲈鱼套餐', desc: '新鲜鲈鱼，清淡鲜美', price: 48, originalPrice: 58, image: '', isNew: true, isHot: false, quantity: 0 },
        { id: 'p5', name: '麻婆豆腐套餐', desc: '麻辣鲜香，下饭神器', price: 22, originalPrice: null, image: '', isNew: false, isHot: true, quantity: 0 },
        { id: 'p6', name: '酸辣土豆丝套餐', desc: '酸辣爽脆，开胃下饭', price: 18, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'p7', name: '可乐鸡翅套餐', desc: '甜香浓郁，肉质鲜嫩', price: 28, originalPrice: null, image: '', isNew: true, isHot: true, quantity: 0 },
        { id: 'p8', name: '酸梅汤', desc: '清凉解腻，酸甜可口', price: 6, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 }
      ],
      rice: [
        { id: 'r1', name: '鱼香肉丝套餐', desc: '经典川菜，酸甜微辣', price: 24, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r2', name: '回锅肉套餐', desc: '川菜经典，香辣下饭', price: 28, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r3', name: '蒜苔炒肉套餐', desc: '蒜香浓郁，肉嫩可口', price: 26, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r4', name: '青椒肉丝套餐', desc: '清脆爽口，营养均衡', price: 24, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r5', name: '番茄炒蛋套餐', desc: '酸甜可口，家常美味', price: 18, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r6', name: '红烧茄子套餐', desc: '软糯入味，素菜经典', price: 20, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r7', name: '干煸豆角套餐', desc: '香辣干爽，下饭必备', price: 22, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'r8', name: '地三鲜套餐', desc: '东北名菜，鲜香可口', price: 24, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 }
      ],
      noodle: [
        { id: 'n1', name: '牛肉面', desc: '大块牛肉，汤浓面劲', price: 22, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'n2', name: '炸酱面', desc: '酱香浓郁，面条劲道', price: 18, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'n3', name: '担担面', desc: '麻辣鲜香，川味十足', price: 16, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'n4', name: '阳春面', desc: '清淡爽口，汤鲜味美', price: 12, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'n5', name: '馄饨', desc: '皮薄馅大，汤鲜味美', price: 15, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'n6', name: '水饺', desc: '手工包制，馅料丰富', price: 18, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 }
      ],
      snack: [
        { id: 's1', name: '炸鸡翅', desc: '外酥里嫩，香脆可口', price: 12, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's2', name: '薯条', desc: '金黄酥脆，蘸酱美味', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's3', name: '鸡米花', desc: '小巧酥脆，一口一个', price: 10, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's4', name: '蛋挞', desc: '外酥里嫩，奶香浓郁', price: 6, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's5', name: '春卷', desc: '外皮酥脆，馅料丰富', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's6', name: '凉拌黄瓜', desc: '清爽解腻，开胃小菜', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's7', name: '凉拌木耳', desc: '爽脆可口，营养健康', price: 10, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 's8', name: '皮蛋豆腐', desc: '清凉爽口，夏日必备', price: 12, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 }
      ],
      drink: [
        { id: 'd1', name: '酸梅汤', desc: '清凉解腻，酸甜可口', price: 6, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd2', name: '柠檬茶', desc: '清新柠檬，茶香四溢', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd3', name: '可乐', desc: '冰爽解渴，经典饮品', price: 6, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd4', name: '雪碧', desc: '清爽柠檬味，解渴首选', price: 6, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd5', name: '橙汁', desc: '鲜榨橙汁，维C满满', price: 12, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd6', name: '西瓜汁', desc: '新鲜西瓜，清凉解暑', price: 10, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd7', name: '红豆沙', desc: '绵密香甜，暖心甜品', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 },
        { id: 'd8', name: '绿豆沙', desc: '清热解暑，夏日必备', price: 8, originalPrice: null, image: '', isNew: false, isHot: false, quantity: 0 }
      ]
    }
    
    const cart = app.getCart('dinein')
    const products = (allProducts[categoryId] || allProducts.hot).map(p => {
      const cartItem = cart.find(c => c.id === p.id)
      return { ...p, quantity: cartItem ? cartItem.quantity : 0 }
    })
    
    this.setData({ products })
  },

  increaseQuantity(e) {
    const id = e.currentTarget.dataset.id
    const product = this.data.products.find(p => p.id === id)
    if (product) {
      app.addToCart(product, 'dinein')
      this.updateCartInfo()
      this.updateProductQuantity(id, 1)
    }
  },

  decreaseQuantity(e) {
    const id = e.currentTarget.dataset.id
    app.removeFromCart(id, 'dinein')
    this.updateCartInfo()
    this.updateProductQuantity(id, -1)
  },

  updateProductQuantity(productId, delta) {
    const products = this.data.products.map(p => {
      if (p.id === productId) {
        return { ...p, quantity: Math.max(0, p.quantity + delta) }
      }
      return p
    })
    this.setData({ products })
  },

  updateCartInfo() {
    this.setData({
      cartCount: app.getCartCount('dinein'),
      cartTotal: app.getCartTotal('dinein').toFixed(2)
    })
  },

  goBack() {
    const pages = getCurrentPages()
    if (pages.length > 1) {
      wx.navigateBack()
    } else {
      wx.reLaunch({
        url: '/pages/index/index'
      })
    }
  },

  goToCart() {
    wx.navigateTo({
      url: '/pages/cart/cart?type=dinein'
    })
  },

  goToConfirm() {
    if (this.data.cartCount === 0) {
      wx.showToast({
        title: '请先选择商品',
        icon: 'none'
      })
      return
    }
    wx.navigateTo({
      url: '/pages/order-confirm/order-confirm?type=dinein&tableNo=' + this.data.tableNo
    })
  }
})
