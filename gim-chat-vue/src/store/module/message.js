/**
 * 📬 Vuex 站内信消息模块
 * - 存储每个联系人或群组的消息列表
 * - 管理每个会话的未读消息数量
 */
import Vue from 'vue';

export default {
  namespaced: true,

  state: {
    messageMap: {},   // { contactId: [message, ...] } 每个会话的消息列表
    unreadMap: {},     // { contactId: unreadCount } 每个会话的未读数
    userList: []      // ✅ 当前用户的联系人/在线用户列表
  },

  mutations: {
    /**
     * ✅ 设置用户列表
     * @param {Array} users 用户数组
     */
    SET_USER_LIST(state, users) {
      console.log('[Vuex:message] 👥 设置用户列表:', users);
      state.userList = users;
    },
    /**
     * ✅ 清空用户列表
     */
    CLEAR_USER_LIST(state) {
      console.warn('[Vuex:message] ⚠️ 清空用户列表');
      state.userList = [];
    },
    /**
     * ✅ 向某个会话追加一条消息
     * @param {String} contactId 会话 ID（用户ID 或 群ID）
     * @param {Object} message 消息对象
     */
    ADD_MESSAGE(state, { contactId, message }) {
      if (!state.messageMap[contactId]) {
        console.log(`[Vuex:message] 初始化会话消息列表: ${contactId}`);
        Vue.set(state.messageMap, contactId, []); // ✅ 响应式新增 key
      }
      console.log(`[Vuex:message] ➕ 添加消息到 ${contactId}:`, message);
      state.messageMap[contactId].push(message);
    },

    /**
     * ✅ 指定会话未读数 +1
     * @param {String} contactId 会话 ID
     */
    INCREMENT_UNREAD(state, contactId) {
      if (!state.unreadMap[contactId]) {
        console.log(`[Vuex:message] 初始化未读计数: ${contactId}`);
        state.unreadMap[contactId] = 0;
      }
      state.unreadMap[contactId]++;
      console.log(`[Vuex:message] 🔴 未读数 +1 (${contactId}):`, state.unreadMap[contactId]);
    },

    /**
     * ✅ 重置指定会话未读消息数为 0（已读）
     * @param {String} contactId 会话 ID
     */
    RESET_UNREAD(state, contactId) {
      console.log(`[Vuex:message] ✅ 清除未读数: ${contactId}`);
      state.unreadMap[contactId] = 0;
    },

    /**
     * ✅ 清空指定会话所有消息
     * @param {String} contactId 会话 ID
     */
    CLEAR_MESSAGES(state, contactId) {
      console.warn(`[Vuex:message] ⚠️ 清空消息: ${contactId}`);
      state.messageMap[contactId] = [];
    }
  },

  getters: {
    /**
     * ✅ 获取用户列表
     */
    getUserList: (state) => {
      return state.userList;
    },
    /**
     * 🟢 获取指定会话的消息数组
     */
    getMessages: (state) => (contactId) => {
      return state.messageMap[contactId] || [];
    },

    /**
     * 🟡 获取指定会话的未读数
     */
    getUnread: (state) => (contactId) => {
      return state.unreadMap[contactId] || 0;
    },

    /**
     * 🔴 获取所有会话的未读消息总和
     */
    getTotalUnread: (state) => {
      const total = Object.values(state.unreadMap).reduce((a, b) => a + b, 0);
      console.log(`[Vuex:message] 🔢 总未读数: ${total}`);
      return total;
    }
  }
};
