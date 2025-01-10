/**
 * Public
 */
export function initialize() {
	buildTopicList();
	initTopicBehavior();
	initItemAction();
}

/**
 */
export function createTopic(topicHtml){
	let topic = {"html":topicHtml, items:[]};
	topicListDef.push(topic);
	
	let addItem = {addItem : (itemHtml) => {
		topic.items.push(itemHtml);
		return addItem;
	}};
	return addItem;
}

/**
 */
export function newTopicHtml(clazz="", text){
	return `<li class="sbar-topic"><span class="${clazz}">${text}</span>`;
}

/**
 */
export function newtItemHtml(id, text){
	return `<li class="sbar-item" id="${id}">${text}</li>`;
}

/**
 */
export function setItemAction(action){
	itemAction = action;
}

/**
 * Internals
 */
let topicListDef = [];
let topicListId = "sidebarTopics";
let itemAction = (id)=>{console.log("No action for: "+id);};

/**
 */
function buildTopicList() {
	if(topicListDef.length>0){
		let html = createTopicListHtml();
		let listElem = document.getElementById(topicListId);
		listElem.innerHTML = html;
	}
}

/**
 */
function createTopicListHtml() {
	let html = "";
	let lines = [];
	
	topicListDef.forEach((topic)=>{
		lines.push(topic.html);
		if(topic.items.length > 0){
			lines.push('<ul class="sbar-item-list">');
			topic.items.forEach((item)=>{
				lines.push(item);
			});
			lines.push('</ul>');
		}
		if(!topic.html.endsWith('</li>')){
			lines.push('</li>');
		}
	});
	html = lines.join("\n");
	return html;
}

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

	for (const item of items) {
		if (item.id && item.id.length > 0) {
			const id = item.id;
			item.addEventListener("click", (evt) => {
				itemAction(id);
				evt.stopImmediatePropagation();
			});
		} else {
			console.log("Warning init item: " + item + " - id:" + item.id);
		}
	}
};
