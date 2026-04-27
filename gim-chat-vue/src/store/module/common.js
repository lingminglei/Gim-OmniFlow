const THEME_STORAGE_KEY = 'gim_theme_mode'

function getInitialThemeMode() {
	try {
		const savedMode = localStorage.getItem(THEME_STORAGE_KEY)
		return savedMode === 'dark' ? 'dark' : 'light'
	} catch (error) {
		return 'light'
	}
}

function persistThemeMode(mode) {
	try {
		localStorage.setItem(THEME_STORAGE_KEY, mode)
	} catch (error) {
		// Ignore storage failures and keep the in-memory state.
	}
}

export default {
	state: {
		screenWidth: document.body.clientWidth,
		themeMode: getInitialThemeMode()
	},
	mutations: {
		/**
		 * 更新屏幕宽度
		 */
		changeScreenWidth(state, data) {
			state.screenWidth = data
		},
		/**
		 * 更新全局主题模式，并同步到本地缓存
		 */
		changeThemeMode(state, mode) {
			const nextMode = mode === 'dark' ? 'dark' : 'light'
			state.themeMode = nextMode
			persistThemeMode(nextMode)
		}
	}
}
