import Vue from 'vue'
import Vuex from 'vuex'
import fileList from './module/fileList' //  文件列表模块
import common from './module/common' //  公共模块
import { allColumnList } from '@/libs/map.js'

import uploadFile from './module/uploadFile' //  拖拽上传文件模块

import message from './module/message'         // ✅ 消息模块
import messageDispatcher from './module/messageDispatcher' // ✅ 消息分发器

import user from './module/user'

Vue.use(Vuex)

export default new Vuex.Store({
	state: {
		//
	},
	getters: {
		// 表格显示列
		selectedColumnList: (state) =>
			state.fileList.selectedColumnList === null
				? allColumnList
				: state.fileList.selectedColumnList.split(','),
		// 文件查看模式
		fileModel: (state) =>
			state.fileList.fileModel === null ? 0 : Number(state.fileList.fileModel),
		// 网格模式 & 时间线模式下 文件图标大小
		gridSize: (state) => state.fileList.gridSize,
	},
	mutations: {
		//
	},
	actions: {
		//
	},
	modules: {
		fileList,
		common,
		uploadFile,
		user,
		message,             // ✅ 注册消息模块
		messageDispatcher    // ✅ 注册消息分发器模块
	}
})
