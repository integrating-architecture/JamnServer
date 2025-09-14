/* Authored by iqbserve.de */

import { ViewBuilder, onClicked } from '../jsmod/view-classes.mjs';
import * as sidebarContent from '../jsmod/sidebar-content.mjs';
import * as Icons from '../jsmod/icons.mjs';

let menuIcon = null;
let collapsed = false;

/**
 * Public
 */
/**
 */
export function initialize(viewManager) {

	//sidebar collaps/menu icon
	menuIcon = Icons.menu(document.getElementById("sidebar.menu.icon")).init((icon) => {
		onClicked(icon.elem, (evt) => {
			toggleCollaps();
		});
	});

	createTopicList(sidebarContent.topicList, viewManager);
}

export function installSidebarHeaderWorkItem(def) {
	let item = null;
	let workIconBar = document.getElementById("sidebar.header.workicons");

	if (def.iconName) {
		item = newWorkIcon(def.id, def.iconName);
		item.elem.title = def.title ? def.title : "";
		workIconBar.append(item.elem);
		onClicked(item.elem, (evt) => {
			def.action(evt);
		});
	}
	return item;
}

/**
 */
export function toggleCollaps() {
	let topics = document.getElementById("sidebar.topic.list");
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
 * Internal
 */
/**
 */
function newWorkIcon(id, iconName) {
	let elem = ViewBuilder.createDomElementFrom(`<i id=${id} class="sidebar-header-icon"></i>`);
	let icon = Icons.newIcon(iconName, elem);
	return icon;
}

/**
 */
function createTopicList(topicListDef, viewManager) {
	let sidebarTopicListElem = document.getElementById("sidebar.topic.list");
	let topicKey = null;
	let topicDef = null;
	let topicElem = null;
	let itemListElem = null;
	let itemKey = null;
	let itemDef = null;
	let itemElem = null;

	for (topicKey in topicListDef) {
		topicDef = topicListDef[topicKey];
		topicElem = newTopic(topicKey, topicDef);
		itemListElem = newTopicList();
		topicElem.append(itemListElem);
		sidebarTopicListElem.append(topicElem);

		for (let key in topicDef.items) {
			itemKey = topicKey + "_" + key;
			itemDef = topicDef.items[key];
			itemElem = newTopicItem(itemDef.id, itemDef.title);
			itemListElem.append(itemElem);
			if (itemDef.view) {
				itemDef.view.onInstallation(itemKey, itemDef.data, viewManager);
				//must be set after onInstallation because view id might change
				itemElem.dataset.viewId = itemDef.view.id;
				viewManager.registerView(itemDef.view, itemDef.data);
				onClicked(itemElem, (evt) => {
					evt.stopPropagation();
					viewManager.onComponentOpenViewRequest(evt.currentTarget.dataset.viewId);
				});
			} else if (itemDef.action) {
				//custom property for direct actions 
				itemElem.action = itemDef.action;
				onClicked(itemElem, (evt) => {
					evt.stopPropagation();
					evt.currentTarget.action();
				});
			}
		}
	}
}

/**
 */
function newTopic(key, def) {

	let iconClazzes = Icons.getIconClasses(def.iconName, true);
	let text = def.title;
	let html = `<li class="sbar-topic"><span class="${iconClazzes}"><span class="sbar-topic-text">${text}</span></span>`;
	let elem = ViewBuilder.createDomElementFrom(html);

	onClicked(elem, (evt) => {
		evt.stopImmediatePropagation();
		let list = evt.currentTarget.lastChild;
		if (list) {
			if (list.style.display == "none" || list.style.display == "") {
				list.style.display = "block";
			} else {
				list.style.display = "none";
			}
		}
	});

	return elem;
}

/**
 */
function newTopicList() {
	let html = `<ul class="sbar-item-list"></ul>`;
	return ViewBuilder.createDomElementFrom(html);
}

/**
 */
function newTopicItem(id, text) {
	id = id ? "id=" + id : "";
	let html = `<li class="sbar-item" ${id}>${text}</li>`;
	return ViewBuilder.createDomElementFrom(html);
}
