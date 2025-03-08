/* Authored by www.integrating-architecture.de */

import * as loginModule from '../jsmod/login.mjs';


let topicListDef = [];
let topicListId = "sidebarTopics";
let itemAction = (id) => { console.warn("Sidebar NO action for: " + id); };
let collapsed = false;

/**
 * Public
 */
export function build() {
	buildTopicList();
	initTopicBehavior();
	initItemAction();
};

/**
 */
export function createTopic(id, topicHtml) {
	let topic = { "id": id, "html": topicHtml, items: [] };
	topicListDef.push(topic);

	let addItem = {
		addItem: (itemHtml) => {
			topic.items.push(itemHtml);
			return addItem;
		}
	};
	return addItem;
};

/**
 */
export function newTopicHtml(iconClazz, text) {
	return `<li class="sbar-topic"><span class="${iconClazz}">${text}</span>`;
};

/**
 * Stores an id value in the value attribute of the item element.
 * This id is pushed to the onClick callback.
 */
export function newtItemHtml(idValue, text) {
	return `<li class="sbar-item" value="${idValue}">${text}</li>`;
};

export function newIdentifiableItemHtml(idValue, text) {
	return `<li class="sbar-item" id="${idValue}">${text}</li>`;
};

/**
 */
export function setItemAction(action) {
	itemAction = action;
};


/**
 */
export function initFunctionalItems(viewmanager) {

	//sidebar collaps icon
	let item = document.getElementById("sidebar.header.menu.icon");
	item.addEventListener("click", (evt) => {
		evt.stopImmediatePropagation();
		toogleCollaps();
	});


	//sidebar login 
	let logInAction = (evt) => {
		evt.stopImmediatePropagation();
		viewmanager.getModalDialog(loginModule.getView(), (dlg) => {
			loginModule.processSystemLogin(dlg);
		});
	}
	//sidebar system login item
	item = document.getElementById("sidebar.system.login");
	item.addEventListener("click", logInAction);

	//sidebar header login icon
	item = document.getElementById("sidebar.header.login.icon");
	item.addEventListener("click", logInAction);
}

/**
 * Internals
 */

/**
 */
function toogleCollaps() {
	let topics = document.getElementById("sidebarTopics");
	let workicons = document.getElementById("sidebar.header.workicons");
	let sidebar = document.getElementById("sidebar");
	let icon = document.getElementById("sidebar.header.menu.icon");

	if (!collapsed) {
		icon.title = "Expand Menu"
		topics.style["display"] = "none";
		workicons.style["display"] = "none";
		sidebar.style["width"] = "50px";
	} else {
		icon.title = "Collapse Menu"
		topics.style["display"] = "block";
		workicons.style["display"] = "flex";
		sidebar.style["width"] = "225px";
	}

	collapsed = !collapsed;
}


/**
 */
function buildTopicList() {
	if (topicListDef.length > 0) {
		let html = createTopicListHtml();
		let listElem = document.getElementById(topicListId);
		listElem.innerHTML = html;
	}
};

/**
 */
function createTopicListHtml() {
	let html = "";
	let lines = [];

	topicListDef.forEach((topic) => {
		lines.push(topic.html);
		if (topic.items.length > 0) {
			lines.push('<ul class="sbar-item-list">');
			topic.items.forEach((item) => {
				lines.push(item);
			});
			lines.push('</ul>');
		}
		if (!topic.html.endsWith('</li>')) {
			lines.push('</li>');
		}
	});
	html = lines.join("\n");
	return html;
};

/**
 * add click behavior show/hide to topics
 */
function initTopicBehavior() {
	let topics = document.getElementsByClassName("sbar-topic");

	for (const item of topics) {
		item.addEventListener("click", (evt) => {
			let list = evt.currentTarget.querySelector('.sbar-item-list');
			if (list) {
				if (list.style.display == "none" || list.style.display == "") {
					list.style.display = "block";
				} else {
					list.style.display = "none";
				}
			}
			evt.stopImmediatePropagation();
		});
	}
};

/**
 * add click actions to items
 * on click call wbApp.openView(View-ID-to-open)
 * a <li class="sbar-item" is expected to have id="View-ID-to-open"
 */
function initItemAction() {
	let items = document.getElementsByClassName("sbar-item");
	let val = null;

	for (const item of items) {
		val = item.getAttribute("value");
		if (val && val.length > 0) {
			//use the value attribute to forword an id to the click action
			const value = item.getAttribute("value");
			item.addEventListener("click", (evt) => {
				itemAction(value);
				evt.stopImmediatePropagation();
			});
		} else if (!item.getAttribute("id")) {
			console.warn("Missing sidebar item value or id: [" + item + "]");
		}
	}
};
