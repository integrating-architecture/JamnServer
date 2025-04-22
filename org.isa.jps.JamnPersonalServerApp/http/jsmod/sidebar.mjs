/* Authored by www.integrating-architecture.de */

import * as loginModule from '../jsmod/login.mjs';
import {IconElement} from '../jsmod/view-classes.mjs';


let topicListDef = [];
let topicListId = "sidebarTopics";
let itemAction = (id) => { console.warn("Sidebar NO action for: " + id); };
let collapsed = false;
let menuIcon = null;
let loginIcon = null;

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
export function newTopicHtml(iconName, text) {
	let iconClazz = IconElement.iconDef(iconName)[0];
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

	//sidebar collaps/menu icon
	menuIcon = IconElement.newIcon("menu", document.getElementById("sidebar.menu.icon"));
	menuIcon.elem.addEventListener("click", (evt) => {
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
	//sidebar header login icon
	loginIcon = IconElement.newIcon("login", document.getElementById("sidebar.login.icon"));
	loginIcon.elem.addEventListener("click", logInAction);

	//sidebar system login item
	let item = document.getElementById("sidebar.system.login");
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

	if (!collapsed) {
		menuIcon.elem.classList.toggle("rot90");
		menuIcon.elem.title = "Expand Menu";
		topics.style["display"] = "none";
		workicons.style["display"] = "none";
		sidebar.style["width"] = "50px";
	} else {
		menuIcon.elem.classList.toggle("rot90");
		menuIcon.elem.title = "Collapse Menu";
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
