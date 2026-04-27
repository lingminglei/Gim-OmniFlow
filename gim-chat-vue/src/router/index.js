import Vue from 'vue';
import VueRouter from 'vue-router';

Vue.use(VueRouter);

const routes = [
  {
    path: '/',
    name: 'home',
    redirect: '/chat'
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'), // 确保路径正确
    meta: { title: '身份验证', public: true } // 标记为公开，不需登录即可访问
  },
    {
    path: '/AIViewOne',
    name: 'AIViewOne',
     component: () => import('../views/utils/aiChat/AIView.vue')
  },

   {
    path: '/EditText',
    name: 'EditText',
     component: () => import('../views/utils/EditText.vue')
  },
   {
    path: '/utils/test',
    name: 'test',
     component: () => import('../views/utils/test.vue')
  },

     {
    path: '/utils/test2',
    name: 'test',
     component: () => import('../views/utils/test2.vue')
  },
  {
    path: '/index',
    name: 'index',
     component: () => import('../views/UserHome.vue')
  },
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/ChatView.vue')
  },
  {
    path: '/group',
    name: 'group',
    component: () => import('../views/GroupView.vue')
  },
  {
    path: '/docs',
    name: 'docs',
    component: () => import('../views/DocsView.vue')
  },
  {
    path: '/ai',
    name: 'ai',
    component: () => import('../views/AIView.vue')
  },
  {
    path: '/knowledge',
    name: 'knowledge',
    component: () => import('../views/utils/KnowledgeView.vue')
  },
  {
    path: '/setting',
    name: 'setting',
    component: () => import('../views/SettingView.vue')
  },
  {
    path: '/orders',
    name: 'orders',
    component: () => import('../views/OrderListView.vue')
  },
  {
    path: '/newList',
    name: 'newList',
    component: () => import('../views/utils/NewList.vue')
  },
  //AI视频
  {
    path: '/aiVideoCreate',
    name: 'aiVideoCreate',
    component: () => import('../views/utils/CreatVideo.vue')
  }
  ,
  //AI视频生成操作流，无线画布版
  {
    path: '/aiVideoCava',
    name: 'aiVideoCava',
    component: () => import('../views/utils/Cava.vue')
  },

   //支付页面
  {
    path: '/pay',
    name: 'pay',
    component: () => import('../views/Pay.vue')
  }
];

const router = new VueRouter({
  mode: 'history',
  base: '/',
  routes
});

// 全局捕获 NavigationDuplicated 错误
const originalPush = VueRouter.prototype.push;
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch((err) => err)
};

export default router;
