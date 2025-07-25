/* Authored by www.integrating-architecture.de */

import { callWebService, typeUtil } from '../jsmod/tools.mjs';
import { WorkView, WorkViewTable, TableData, ViewBuilder } from '../jsmod/view-classes.mjs';
import * as webapi from '../jsmod/webapi.mjs';

let builder = new ViewBuilder();
builder.defaultStyles.label = { "min-width": "80px", "text-align": "right" };
let boxWidth = "720px";

let app_scm_tab1 = "?tab=readme-ov-file#jamn---just-another-micro-node-server";

/**
 * Concrete view class for the info component
 */
class SystemInfoView extends WorkView {

	appBoxElems = {};

	configBoxElems = {};
	configTable;

	needsViewDataRefresh = true;

	initialize() {
		super.initialize();
		this.setTitle("System Infos");

		this.#initAppBox();
		this.#initConfigBox();

		this.isInitialized = true;
	}

	open() {
		super.open();
		getInfos((data) => {
			this.writeDataToView(data);
			this.setVisible(true);
		});
	}

	/**
	 */
	#initAppBox() {

		builder.setElementCollection(this.appBoxElems);

		let compSet = builder.newFieldset({ title: "Application", clazzes: ["wkv-compset-border"], styleProps: { width: boxWidth } });
		let infoContainer = this.getElement("info.left.container");
		infoContainer.prepend(compSet);

		builder.newViewComp()
			.addLabelTextField({ text: "Name:" }, { varid: "tfName", readOnly: true, 
				styleProps:{"font-size": "18px", color: "var(--isa-title-grayblue)"}})
			.appendTo(compSet);

		builder.newViewComp()
			.addLabelTextField({ text: "Version:" }, { varid: "tfVersion", readOnly: true })
			.appendTo(compSet);

		builder.newViewComp()
			.addLabel({ text: "Description:" })
			.addContainer({ clazzes: "wkv-col-container", styleProps: { width: "100%" } }, (target) => {
				let container = target.container;

				target.comp.addTextArea({
					parentCtrl: container, varid: "tfDescription", rows: 3, readOnly: true
				});
				target.comp.addLink({
					parentCtrl: container, varid: "lnkReadMore", text: "Read more on GitHub ... ",
					attribProps: { title: "Jamn Personal Server - All-In-One MicroService App", target: "_blank" },
					styleProps: { "text-align": "right" }
				});

			})
			.style({ "align-items": "baseline", "padding-right": "5px" })
			.appendTo(compSet);
	}

	/**
	 */
	#initConfigBox() {

		builder.setElementCollection(this.configBoxElems);

		//get the html coded configSet element
		let compSet = this.getElement("server.config.set");
		ViewBuilder.setStyleOf(compSet, { "padding-top": "10px", width: boxWidth });

		builder.newViewComp()
			.prependTo(compSet)
			.style({ "flex-direction": "row-reverse", "margin-bottom": "10px", "gap": "15px" })
			//create action icon
			.addActionIcon({ varid: "icoSave", iconName: "save", title: "Save current changes" }, (target) => {
				target.icon.onclick = () => {
					updateInfos(getUpdateRequest(), (response) => {
						if (response?.status === "ok") {
							clearConfigChanges()
							console.log("App-Info update done");
						}
					});
				}
			})
			//create action icon
			.addActionIcon({ varid: "icoRedo", iconName: "redo", title: "Undo changes" }, (target) => {
				target.icon.onclick = () => {
					//open confirmation dialog
					WbApp.confirm({
						message: "<b>Undo all changes</b><br>Do you want to discard all changes?"
					}, (value) => value ? clearConfigChanges(true) : null);
				};
			});

		this.setActionsEnabled(false);

		//create a table
		this.configTable = new WorkViewTable(this.getElement("server.config"));
	}

	/**
	 */
	setActionsEnabled(flag) {
		let ctrls = [this.configBoxElems["icoSave"], this.configBoxElems["icoRedo"]];
		let styleProps = flag ? { "pointer-events": "all", color: "" } : { "pointer-events": "none", color: "var(--border-gray)" };

		ctrls.forEach((ctrl) => ViewBuilder.setStyleOf(ctrl, styleProps));
	}

	/**
	 */
	writeDataToView(data) {
		if (this.needsViewDataRefresh) {
			clearConfigChanges();

			this.appBoxElems["tfName"].value = data.name;
			this.appBoxElems["tfVersion"].value = `${data.version} - Build [${data.buildDate} UTC]`;
			this.appBoxElems["tfDescription"].value = data.description;
			this.appBoxElems["lnkReadMore"].href = data.links["app.scm"] + app_scm_tab1;

			//create+build a table data object
			let tableData = new TableData();
			// "data.config" has the structure: { name1:value1, name2:value2 ... }
			// create a 2 column tableData from it
			let names = Object.getOwnPropertyNames(data.config);
			names.forEach((name) => {
				let row = new Map();
				//mark the read only key column to filter out 
				row.set("key:" + name, name);
				row.set(name, data.config[name]);
				tableData.addRow(name, row);
			})

			//define cell editing on double click
			tableData.cellDblClick = (rowKey, colKey, evt) => {

				//editing only for the value column
				if (!colKey.startsWith("key:")) {
					//get the origin data from the data object (model)
					let dataRow = tableData.rows.get(rowKey);
					let dataValue = dataRow.get(colKey);
					console.log(dataValue);

					//create+handle a simple cell input field
					let cellElem = evt.currentTarget;
					if (cellElem.getElementsByTagName('input').length > 0) return;
					//for simplicity use the html table cell value
					let orgCellValue = cellElem.innerHTML;
					cellElem.innerHTML = '';

					let inputFieldProps = {};
					inputFieldProps.booleanValue = typeUtil.booleanFromString(orgCellValue);
					let cellInput = this.configTable.newCellInputField(inputFieldProps);
					cellInput.value = orgCellValue;

					cellInput.onblur = (evt) => {
						let newValue = cellInput.value;
						cellElem.removeChild(cellInput.comp);
						if (typeUtil.isBooleanString(newValue) && !typeUtil.isBooleanString(orgCellValue)) {
							newValue = orgCellValue;
						}
						cellElem.innerHTML = newValue !== orgCellValue ? newValue : orgCellValue;
						ckeckConfigChange(colKey, dataValue, cellElem);
					};

					cellInput.onkeydown = (evt) => {
						if (evt.keyCode == 13) {
							cellInput.blur();
						} else if (evt.keyCode == 27) {
							cellInput.blur();
							cellElem.innerHTML = orgCellValue;
							ckeckConfigChange(colKey, dataValue, cellElem);
						}
					};

					cellElem.appendChild(cellInput.comp);
					cellInput.focus();
				}
			};

			this.configTable.setData(tableData);
			this.configTable.sortByColumn(0);

			this.configTable.getHeader(0).getElementsByTagName("i")[0].onclick = (evt) => {
				this.configTable.sortByColumn(0);
				this.configTable.toggleColSort(0);
			};

			this.configTable.getHeader(0).getElementsByTagName("input")[0].onkeyup = (evt) => {
				this.configTable.filterRows(0, evt.target.value);
			};

			this.needsViewDataRefresh = false;
		}
	}
}

//export this view component as singleton instance
const viewInstance = new SystemInfoView("systemInfoView", "/jsmod/html-components/system-infos.html");
export function getView() {
	return viewInstance;
}
let configChanges = new Map();
let infoData = null;

/**
 */
export function getInfos(cb) {
	if (infoData) {
		cb(infoData);
	} else {
		//load the infos from server
		callWebService(webapi.system_getinfos).then((data) => {
			infoData = data;
			cb(infoData);
		});
	}
}

/**
 */
function updateInfos(request, cb) {
	//send changes to the server
	callWebService(webapi.system_updateinfos, JSON.stringify(request)).then((response) => {
		cb(response);
	});
}

/**
 */
function clearConfigChanges(undo = false) {
	configChanges.forEach((cell, key) => {
		cell.elem.style["border-left"] = "";
		if (undo) { cell.elem.innerHTML = cell.orgData; };
	});
	configChanges.clear();
	getView().setActionsEnabled(false);
}

/**
 */
function ckeckConfigChange(key, orgVal, cellElem) {
	let currentVal = cellElem.innerHTML;

	if (orgVal !== currentVal) {
		configChanges.set(key, { elem: cellElem, orgData: orgVal });
		cellElem.style["border-left"] = "3px solid #32cd32";
	} else {
		configChanges.delete(key);
		cellElem.style["border-left"] = "";
	}
	getView().setActionsEnabled(configChanges.size !== 0);
}

/**
 */
function getUpdateRequest() {
	let request = { "configChanges": {} };
	configChanges.forEach((cell, key) => {
		request.configChanges[key] = cell.elem.innerHTML;
	});
	return request;
}
