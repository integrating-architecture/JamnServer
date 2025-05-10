/* Authored by www.integrating-architecture.de */

import { NL, getChildOf, setVisibility, setDisplay, typeUtil } from '../jsmod/tools.mjs';
import { ViewSource, CommandDef } from '../jsmod/data-classes.mjs';

/**
 * View base classes.
 * 
 * View classes encapsulate a basic structure and a basic open close procedure 
 * in the form of corresponding methods and flags.
 * 
 */
export class GeneralView {

	id = "";
	viewSource = new ViewSource("");
	viewManager = null;

	//the view html dom element
	viewElement = null;
	viewTitle = null;
	viewWorkarea = null;

	isInitialized = false;
	isOpen = false;

	constructor(id, file) {
		this.id = id;
		this.viewSource = new ViewSource(file);

		this.isInitialized = false;
		this.isOpen = false;
	}

	setViewManager(viewManager) {
		this.viewManager = viewManager;
	}

	open(data = null) {
		if (!this.isInitialized) {
			this.initialize();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;
	}

	close() {
		this.isOpen = false;
	}

	initialize() {
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");
	}

	writeDataToView() {
		//to be overwritten
		//always called in showView
	}

	readDataFromView() {
		//to be overwritten
	}

	setTitle(title) {
		this.viewTitle.innerHTML = title;
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

}

/**
 * A class to centralize font icon usage.
 */
export class IconElement {
	elem = null;
	type = "";
	shapes = ["", ""];

	constructor(elem, type, shapes) {
		this.elem = elem;
		this.type = type;
		this.shapes = shapes;
	}

	init(cb = (obj) => { }) {
		cb(this);
		return this;
	}

	toggle() {
		this.elem.classList.toggle(this.shapes[0]);
		this.elem.classList.toggle(this.shapes[1]);
		return this;
	}

	and(cb) {
		cb(this.elem);
	}

	static newIcon(name, elem, type = "bi") {
		return new IconElement(elem, type, IconElement.#Icons[type][name]).init(IconElement.#Icons[type].defaultInit);
	}

	static iconDef(name, type = "bi") {
		return IconElement.#Icons[type][name];
	}

	static #Icons = {
		bi: {
			defaultInit: (iconObj) => {
				//Bootstrap font icons
				iconObj.elem.classList.add(iconObj.type ? iconObj.type : "bi");
				iconObj.elem.classList.add(iconObj.shapes[0]);
			},
			close: ["bi-x-lg", ""],
			pin: ["bi-pin", "bi-pin-angle"],
			collapse: ["bi-chevron-bar-contract", "bi-chevron-bar-expand"],
			dotmenu: ["bi-three-dots-vertical", ""],
			menu: ["bi-list", ""],
			login: ["bi-person", "bi-person-check"],
			github: ["bi-github", ""],
			system: ["bi-laptop", ""],
			command: ["bi-command", ""],
			tools: ["bi-tools", ""],
			user: ["bi-person", ""],
			password: ["bi-key", ""],
			loginAction: ["bi-box-arrow-in-right", ""],
			tableSort: ["bi-arrow-down", "bi-arrow-up"],
			save: ["bi-floppy", ""],
			redo: ["bi-arrow-counterclockwise", ""],
			addnew: ["bi-plus-square", ""],
			trash: ["bi-trash", ""],
			clipboardAdd: ["bi-clipboard-plus", ""]
		}
	}
}

/**
 * Work View base class.
 */
export class WorkView {

	id = "";
	viewSource = new ViewSource("");
	viewManager = null;

	//the view html dom element
	viewElement = null;
	viewTitle = null;
	viewWorkarea = null;

	//header elements
	closeIcon = null;
	collapseIcon = null;
	pinIcon = null;
	menuIcon = null;

	headerMenu = null;

	isInitialized = false;
	isRunning = false;
	isOpen = false;
	isPinned = false;
	isCollapsed = false;

	constructor(id, file) {
		this.id = id;
		this.viewSource = new ViewSource(file);

		this.isInitialized = false;
		this.isRunning = false;
		this.isOpen = false;
	}

	initialize() {
		//to be overwritten
		//only called when isInitialized=false
		this.viewElement = document.getElementById(this.id);
		this.viewTitle = this.getElement("view.title");
		this.viewWorkarea = this.getElement("work.view.workarea");

		this.closeIcon = IconElement.newIcon("close", this.getElement("close.icon"));
		this.pinIcon = IconElement.newIcon("pin", this.getElement("pin.icon"));
		this.collapseIcon = IconElement.newIcon("collapse", this.getElement("collapse.icon"));
		this.menuIcon = IconElement.newIcon("dotmenu", this.getElement("menu.icon"));

		let elem = this.getElement("header.menu");
		if (elem && this.closeIcon.elem) {
			this.headerMenu = new WorkViewHeaderMenu(elem);

			this.headerMenu.addItem("Close", (evt) => {
				this.closeIcon.elem.click();
			}, { separator: "bottom" });

			if (this.viewManager) {
				this.headerMenu.addItem("Move up", (evt) => {
					this.viewManager.moveView(this, "up");
				});
				this.headerMenu.addItem("Move down", (evt) => {
					this.viewManager.moveView(this, "down");
				});
				this.headerMenu.addItem("Move to ...", (evt) => {
					this.viewManager.promptUserInput({ title: "", message: "Please enter your desired position number:" }, "1",
						(value) => value ? this.viewManager.moveView(this, value) : null
					);
				});
			}
		}
	}

	open(data = null) {
		if (!this.isInitialized) {
			this.initialize();
		}
		if (this.headerMenu) {
			this.headerMenu.close();
		}
		this.writeDataToView();
		setVisibility(this.viewElement, true);
		this.isOpen = true;
	}

	close() {
		this.isOpen = false;
		if (this.headerMenu) {
			this.headerMenu.close();
		}
		return this.isCloseable();
	}

	isCloseable(ctxObj = null) {
		return !(this.isRunning || this.isPinned);
	}

	writeDataToView() {
		//to be overwritten
		//always called in showView
	}

	readDataFromView() {
		//to be overwritten
	}

	setRunning(flag) {
		this.isRunning = flag;
	}

	setTitle(title) {
		this.viewTitle.innerHTML = title;
	}

	getElement(id) {
		return getChildOf(this.viewElement, id);
	}

	onInstallation(installKey, installData, viewManager) {
		this.viewManager = viewManager;
	}

	togglePinned() {
		this.isPinned = !this.isPinned;

		this.pinIcon.toggle().and((icon) => {
			icon.title = this.isPinned ? "Unpin view" : "Pin to keep view";

			if (this.isPinned) {
				this.closeIcon.elem.style["pointer-events"] = "none";
			} else {
				this.closeIcon.elem.style["pointer-events"] = "all";
			}
		});

		return this.isPinned;
	}

	toggleCollapsed() {
		this.isCollapsed = !this.isCollapsed;

		this.collapseIcon.toggle().and((icon) => {
			icon.title = this.isCollapsed ? "Expand view" : "Collapse  view";

			setDisplay(this.viewWorkarea, !this.isCollapsed);
		});

		return this.isCollapsed;
	}

	toggleHeaderMenu() {
		if (!this.isCollapsed) {
			this.headerMenu.toggleVisibility();
		}
	}

	statusLineInfo(info) {
		WbApp.statusLineInfo(info);
	}
}


/**
 * A base class for command views
 * that get parameterized with a commandDef object on installation
 */
export class BaseCommandView extends WorkView {

	commandDef = new CommandDef();
	commandName = "";

	workIndicator = null;

	//expected to be CtrlComp objects
	runButton = null;
	runArgs = null;
	outputArea = null;

	onInstallation(installKey, installData, viewManager) {
		super.onInstallation(installKey, installData, viewManager);
		this.id = installKey;
		if (installData instanceof CommandDef) {
			this.commandDef = installData;
			this.commandName = this.commandDef.command + " " + this.commandDef.script;
		}
	}

	initialize() {
		super.initialize();

		this.setTitle(this.commandDef.title);
		this.workIndicator = this.getElement("work.indicator");

		if (this.headerMenu) {
			this.headerMenu.addItem("Clear Output", (evt) => {
				this.clearOutput();
			}, { separator: "top" });
		}
	}

	clearOutput() {
		if (!this.isRunning) {
			let lastValue = this.outputArea.ctrl().value;
			this.outputArea.ctrl().value = "";
			return lastValue;
		}
		return "";
	}

	copyOutputToClipboard() {
		let outputData = this.outputArea.ctrl().value.trim();
		if (!this.isRunning && outputData.length > 0) {
			navigator.clipboard.writeText(outputData);
		}
	}

	saveOutput() {
		let outputData = this.outputArea.ctrl().value.trim();
		if (!this.isRunning && outputData.length > 0) {
			let fileName = "output_" + (this.commandDef.command + "_" + this.commandDef.script).replaceAll("/", "_") + ".txt";
			window.showSaveFilePicker({
				suggestedName: fileName,
				types: [{
					description: "Text file",
					accept: { "text/plain": [".txt"] },
				}],
			}).then(async handler => {
				let file = await handler.createWritable();
				await file.write(outputData);
				await file.close();
			}).catch(err => console.error(err));
		}
	}

	addOutputLine(line) {
		this.outputArea.ctrl().value += line + NL;
		this.outputArea.ctrl().scrollTop = this.outputArea.ctrl().scrollHeight;
	}

	setRunning(flag) {
		this.isRunning = flag;

		setVisibility(this.workIndicator, flag);
		this.runButton.ctrl().disabled = flag;
	}
}

/**
 */
export class WorkViewHeaderMenu {

	containerElem = null;
	menuElem = null;
	isVisible = false;

	constructor(containerElem) {
		this.containerElem = containerElem;
		this.menuElem = containerElem.children[0];

		window.addEventListener("click", (event) => {
			this.onAnyWindowClick(event)
		});
	}

	hasItems() {
		return this.menuElem?.children.length > 0;
	}

	close() {
		this.isVisible = false;
		setDisplay(this.menuElem, this.isVisible);
	}

	addItem(text, cb, props = {}) {
		let item = document.createElement("a");
		item.id = props?.id;
		item.href = "javascript:void(0)";
		item.innerHTML = text;

		if (props?.separator) {
			let clazz = props.separator === "top" ? "menu-separator-top" : "menu-separator-bottom";
			item.classList.add(clazz);
		}
		item.onclick = (evt) => cb(evt);

		this.menuElem.appendChild(item);
	}

	toggleVisibility() {
		if (this.hasItems()) {
			this.isVisible = !this.isVisible
			setDisplay(this.menuElem, this.isVisible);
		}
	}

	onAnyWindowClick(event) {
		this.close();
	}
}

/**
 */
export class ModalDialog {
	containerElem;
	header;
	title;
	closeIcon;
	viewArea;
	commandArea;

	constructor(containerElem) {
		this.containerElem = containerElem;
		this.header = getChildOf(containerElem, "modal.dialog.header");
		this.title = getChildOf(this.header, "dialog.title");
		this.viewArea = getChildOf(containerElem, "modal.dialog.view.area");
		this.commandArea = getChildOf(containerElem, "modal.dialog.command.area");

		this.closeIcon = IconElement.newIcon("close", getChildOf(containerElem, "modal.dialog.close.icon"));
		this.closeIcon.elem.addEventListener("click", () => {
			this.close();
		});
	}

	//called from viewManager on open request
	setViewHtml(html) {
		this.viewArea.innerHTML = html;
		return this;
	}

	close() {
		setDisplay(this.containerElem, false);
		this.viewArea.innerHTML = "";
	}

	setTitle(title) {
		this.title.innerHTML = title;
		return this;
	}

	setAction(id, action) {
		getChildOf(this.containerElem, id).onclick = action;
		return this;
	}

	setElement(id, cb) {
		cb(getChildOf(this.containerElem, id));
		return this;
	}

	open() {
		setDisplay(this.containerElem, true);
		return this;
	}

	hideArea(area) {
		setDisplay(area, false);
		return this;
	}
}

/**
 */
export class StandardDialog {
	dialogElem;
	contentArea;
	commandArea;
	title;
	closeIcon;
	pbOk;
	pbCancel;
	tfInput;

	constructor() {
		this.dialogElem = document.getElementById("standardDialog");
		this.contentArea = getChildOf(this.dialogElem, "standard.dialog.content.area");
		this.commandArea = getChildOf(this.dialogElem, "standard.dialog.command.area");
		this.title = getChildOf(this.dialogElem, "standard.dialog.title");
		this.pbOk = getChildOf(this.dialogElem, "pb.standard.dialog.ok");
		this.pbCancel = getChildOf(this.dialogElem, "pb.standard.dialog.cancel");

		this.closeIcon = IconElement.newIcon("close", getChildOf(this.dialogElem, "standard.dialog.close.icon"));
	}

	openConfirmation(text, cb) {
		this.setupFor("confirm", text, null, cb);
		this.dialogElem.showModal();
	}

	openInput(text, value, cb) {
		this.setupFor("input", text, value, cb);
		this.dialogElem.showModal();
		this.tfInput.focus();
		this.tfInput.select();
	}

	setupFor(type, text, value, cb) {

		this.pbOk.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(type === "input" ? this.tfInput.value : true);
		};

		this.closeIcon.elem.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(null);
		};
		this.pbCancel.onclick = (evt) => {
			evt.stopImmediatePropagation();
			this.dialogElem.close();
			cb(null);
		};

		if (type === "confirm") {
			this.setupForConfirm(text);
		} else if (type === "input") {
			this.setupForInput(text, value);
		}
	}

	setupForConfirm(text) {
		this.pbOk.innerHTML = "Yes";
		this.pbCancel.innerHTML = "No";
		let title = "Confirmation required";

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = title;
			this.contentArea.innerHTML = `<p>${text}</p>`;
		} else {
			this.title.innerHTML = text.title ? text.title : title;
			this.contentArea.innerHTML = `<p>${text?.message}</p>`;
		}
	}

	setupForInput(text, value) {
		this.pbOk.innerHTML = "Ok";
		this.pbCancel.innerHTML = "Cancel";
		let title = "Input";

		if (!value) { value = "" };

		if (typeof text === 'string' || text instanceof String) {
			this.title.innerHTML = title;
			this.contentArea.innerHTML = `<p class="std-dlg-input">${text}</p> 
			<input type="text" id="tf.standard.dialog.input" class="standard-dialog-textfield" value=${value}>`;
		} else {
			this.title.innerHTML = text.title ? text.title : title;
			this.contentArea.innerHTML = `<p class="std-dlg-input">${text?.message}</p>
			<input type="text" id="tf.standard.dialog.input" class="standard-dialog-textfield" value=${value}>`;
		}

		this.tfInput = getChildOf(this.dialogElem, "tf.standard.dialog.input");
	}

}

/**
 */
export class WorkViewTable {
	tableElem;
	tableBody;
	tableData = null;
	ascOrder = false;
	sortIcon;

	constructor(tableElem) {
		this.tableElem = tableElem;
		this.tableBody = this.tableElem.querySelector('tbody');

		let icon = this.getHeader(0).getElementsByTagName("i")[0];
		this.sortIcon = IconElement.newIcon("tableSort", icon);
	}

	getHeader(idx) {
		return this.tableElem.getElementsByTagName("th")[idx];
	}

	setData(tableData) {
		this.clearData();
		this.tableData = tableData;

		this.tableData.rows.forEach((rowData, rowKey) => {
			let row = document.createElement("tr");
			row.className = "wkv";

			rowData.forEach((colVal, colKey) => {
				let col = document.createElement("td");
				col.className = "wkv";
				col.innerHTML = colVal;
				col.value = colKey;
				col.onclick = (evt) => { this.tableData.cellClick(rowKey, colKey, evt); };
				col.ondblclick = (evt) => { this.tableData.cellDblClick(rowKey, colKey, evt); };
				row.appendChild(col);
			});

			this.tableBody.appendChild(row);
		});
	}

	clearData() {
		this.tableData = null;
		this.tableBody.replaceChildren();
	}

	sortByColumn(colIdx) {
		this.ascOrder = !this.ascOrder;
		let rows = Array.from(this.tableBody.querySelectorAll('tr'));

		rows.sort((rowA, rowB) => {
			let cellA = rowA.querySelectorAll('td')[colIdx].textContent.trim();
			let cellB = rowB.querySelectorAll('td')[colIdx].textContent.trim();

			return this.ascOrder ? cellA.localeCompare(cellB) : cellB.localeCompare(cellA);
		});

		this.tableBody.replaceChildren();
		this.tableBody.append(...rows);
	}

	toggleColSort(colIdx) {
		this.sortIcon.toggle();
	}

	filterRows(colIdx, filterText) {
		let rows = Array.from(this.tableBody.querySelectorAll('tr'));
		let filter = filterText.toLowerCase();

		rows.forEach((row) => {
			let cellVal = row.querySelectorAll('td')[colIdx].textContent;
			row.style.display = cellVal.toLowerCase().indexOf(filter) < 0 ? "none" : "";
		});
	}

	newCellInputField(props = { clazz: "wkv-tblcell-edit-tf", booleanValue: null, datalist: [] }) {
		let comp = document.createElement('span');
		let ctrl = document.createElement('input');

		ctrl.comp = comp;
		ctrl.type = "text";
		ctrl.classList.add(props.clazz ? props.clazz : "wkv-tblcell-edit-tf");
		comp.append(ctrl);

		if (props?.booleanValue != null) {
			ctrl.type = "checkbox";
			ctrl.checked = props.booleanValue;
			ctrl.style.width = "20px";
			ctrl.onclick = (evt) => { ctrl.value = typeUtil.stringFromBoolean(ctrl.checked) };
		} else if (props.datalist?.length > 0) {
			let item = null;
			let dataElem = document.createElement("datalist");
			dataElem.id = Math.random().toString(32).slice(5);
			props.datalist.forEach(entry => {
				item = document.createElement("option");
				item.value = entry;
				dataElem.append(item);
			});
			ctrl.setAttribute("list", dataElem.id);
			comp.append(dataElem);
		}
		return ctrl;
	}
}

/**
 * Table data represented as 
 * - map of rows (key:row)
 *  - each row a map of columns (key:column)
 */
export class TableData {
	rows;
	cellClick;
	cellDblClick;

	constructor() {
		this.rows = new Map();
		this.cellClick = (rowKey, colKey, evt) => { };
		this.cellDblClick = (rowKey, colKey, evt) => { };
	}

	addRow(key, columns) {
		this.rows.set(key, columns);
	}
}

/**
 * An experimental factory/builder to create standard UI components e.g. like
 *  [label] - [textfield] etc.
 * arranged e.g. in a fieldset container.
 * 
 * The builder just provides the basic html elements
 * and returns a "proxy" object (CtrlComp) that provides styling, attribution etc. methods
 * to enable a cascading builder style programming format.
 */
export class CompBuilder {

	//provide default style classes
	textCompClasses = ["wkv-label-ctrl", "wkv-value-ctrl"];
	textAreaCompClasses = ["wkv-label-ctrl", "wkv-textarea-ctrl"];
	buttonCompClasses = ["wkv-label-ctrl", "wkv-button-ctrl"];

	labelStyle = null;

	static style(elem, styleProps) {
		for (const name in styleProps) {
			elem.style[name] = styleProps[name];
		}
	}

	#idCheck(id) {
		if (!id || id === 'undefined' || id === "") {
			//if no id - create a random one
			return Math.random().toString(32).slice(5);
		}
		return id;
	}

	#setClassOf(ctrl, compProps, defaultClass) {
		if (compProps?.clazz) {
			if (Array.isArray(compProps.clazz)) {
				compProps.clazz.forEach(clazz => ctrl.classList.add(clazz));
			} else {
				ctrl.classList.add(compProps.clazz);
			}
		} else {
			ctrl.classList.add(defaultClass);
		}
	}

	#newLabel(props, clazz) {
		let ctrl = document.createElement("label");
		ctrl.classList.add(clazz);
		ctrl.innerHTML = props.label;
		ctrl.htmlFor = props.id;

		if (this.labelStyle !== null) {
			for (const name in this.labelStyle) {
				ctrl.style[name] = this.labelStyle[name];
			}
		}
		return ctrl;
	}

	//creation options for individual overwriting
	newCompSet(opt = { tag: "fieldset", clazz: "wkv-compset", title: null, styleProps: {}, clazzes: [] }) {
		let compSet = document.createElement(opt.tag ? opt.tag : "fieldset");
		compSet.classList.add(opt.clazz ? opt.clazz : "wkv-compset");

		if (opt.title) {
			let legend = document.createElement("legend");
			legend.innerHTML = opt.title;
			compSet.append(legend);
		}

		if (opt.styleProps) {
			for (const name in opt.styleProps) {
				compSet.style[name] = opt.styleProps[name];
			}
		}

		if (opt.clazzes) {
			opt.clazzes.forEach(clazz => compSet.classList.add(clazz));
		}

		return compSet;
	}

	/**
	 * provide standard, pre configured components as CtrlComp objects
	 * compProps overwrite generalProps 
	 */
	newTextComp(compProps = { label: "", id: "", readOnly: false }, generalProps = {}) {
		let props = { ...generalProps, ...compProps };
		let ctrls = [];
		props.id = this.#idCheck(props.id);
		ctrls[0] = this.#newLabel(props, this.textCompClasses[0]);

		ctrls[1] = document.createElement("input");
		ctrls[1].type = "text";
		ctrls[1].id = props.id;

		this.#setClassOf(ctrls[1], props, this.textCompClasses[1]);

		if (props.readOnly) {
			ctrls[1].classList.add("input-readonly");
			ctrls[1].disabled = true;
		}

		return new CtrlComp(ctrls, "text");
	}

	/**
	 */
	newTextDatalistComp(compProps = { label: "", id: "", datalist: [], readOnly: false }, generalProps = {}) {
		let props = { ...generalProps, ...compProps };
		let ctrls = [];
		props.id = this.#idCheck(props.id);
		ctrls[0] = this.#newLabel(props, this.textCompClasses[0]);

		ctrls[1] = document.createElement("input");
		ctrls[1].type = "text";
		ctrls[1].id = props.id;

		this.#setClassOf(ctrls[1], props, this.textCompClasses[1]);

		if (props.readOnly) {
			ctrls[1].classList.add("input-readonly");
			ctrls[1].disabled = true;
		}

		let dataElem = document.createElement("datalist");
		dataElem.id = "data." + props.id;
		if (props.datalist) {
			let item = null;
			props.datalist.forEach(entry => {
				item = document.createElement("option");
				item.value = entry;
				dataElem.append(item);
			});
		}
		ctrls[1].setAttribute("list", dataElem.id);

		let comp = new CtrlComp(ctrls, "text");
		comp.datalist = dataElem;
		return comp;
	}

	newTextAreaComp(compProps = { label: "", id: "", readOnly: false }, generalProps = {}) {
		let props = { ...generalProps, ...compProps };
		let ctrls = [];
		props.id = this.#idCheck(props.id);
		ctrls[0] = this.#newLabel(props, this.textAreaCompClasses[0]);

		ctrls[1] = document.createElement("textarea");
		ctrls[1].id = props.id;
		ctrls[1].rows = props.rows;

		this.#setClassOf(ctrls[1], props, this.textAreaCompClasses[1]);

		if (props.readOnly) {
			ctrls[1].classList.add("textarea-readonly");
			ctrls[1].disabled = true;
		}

		return new CtrlComp(ctrls, "textarea");
	}

	newButtonComp(compProps = { label: "", id: "", enabled: true }, generalProps = {}) {
		let props = { ...generalProps, ...compProps };
		let ctrls = [];
		props.id = this.#idCheck(props.id);
		ctrls[0] = this.#newLabel(props, this.buttonCompClasses[0]);

		ctrls[1] = document.createElement("input");
		ctrls[1].type = "button";
		ctrls[1].id = props.id;

		this.#setClassOf(ctrls[1], props, this.buttonCompClasses[1]);

		return new CtrlComp(ctrls, "button");
	}

	newIconbarComp(compProps = { icons: [] }, generalProps = {}) {
		let props = { ...generalProps, ...compProps };
		let ctrls = [];
		props.id = this.#idCheck(props.id);

		compProps.icons.forEach(item => {
			let elem = document.createElement("i");
			elem.setAttribute("title", item?.title);
			IconElement.newIcon(item.name, elem);
			ctrls.push(elem);
		});

		return new CtrlComp(ctrls, "icon");
	}
}

/**
 * The component object provides the preconfigured dom elements
 * and methods for "configuration/appending" of the ui controls. 
 */
export class CtrlComp {
	type = "";
	comp = null;
	ctrls = [];
	datalist = null;

	constructor(ctrls, type = "text") {
		this.type = type;
		this.ctrls = ctrls;
	}

	style(idx, styleProps) {
		let target = null;
		let values = styleProps;
		//if idx is a style object use it for the enclosing component itself
		if (isNaN(idx) && typeof idx === 'object') {
			target = this.comp;
			values = idx;
		} else {
			//else use it for an inner control
			target = this.ctrls[idx];
		}
		for (const name in values) {
			target.style[name] = values[name];
		}
		return this;
	}

	attrb(idx, attributeProps) {
		let target = null;
		let values = attributeProps;
		if (isNaN(idx) && typeof idx === 'object') {
			target = this.comp;
			values = idx;
		} else {
			target = this.ctrls[idx];
		}
		for (const name in values) {
			target[name] = values[name];
		}
		return this;
	}

	config(cb) {
		cb(this);
		return this;
	}

	/**
	 * @returns the "main control" e.g. input, button etc.
	 */
	ctrl() {
		return this.ctrls[1];
	}

	build(clazz = "wkv-ctrlcomp") {
		if (this.comp === null) {
			this.comp = document.createElement("span");
			clazz = Array.isArray(clazz) ? clazz : [clazz];
			clazz.forEach(cls => this.comp.classList.add(cls));
			this.ctrls.forEach(ctrl => this.comp.append(ctrl));
			if (this.datalist) {
				this.comp.append(this.datalist);
			}
		}
		return this;
	}

	addComp(otherComp) {
		this.build();
		otherComp.ctrls.forEach(ctrl => { this.comp.append(ctrl); this.ctrls.push(ctrl); });
		return this;
	}

	appendTo(parent) {
		this.build();
		parent.append(this.comp);
		return this;
	}

	prependTo(parent) {
		this.build();
		parent.prepend(this.comp);
		return this;
	}
}

