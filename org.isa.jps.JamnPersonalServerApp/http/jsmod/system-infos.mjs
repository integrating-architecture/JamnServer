/* Authored by www.integrating-architecture.de */

import { callWebService, typeUtil } from '../jsmod/tools.mjs';
import { WorkView, WorkViewTable, TableData, CompBuilder } from '../jsmod/view-classes.mjs';

let builder = new CompBuilder();
let boxWidth = "720px";

/**
 * Concrete view class for the info component
 */
class SystemInfoView extends WorkView {

	name;
	version;
	description;
	configTable;
	configBar;

	needsDataReload = true;

	initialize() {
		super.initialize();
		this.setTitle("System Infos");

		this.#initAppBox();
		this.#initConfigBox();

		this.isInitialized = true;
	}

	/**
	 */
	#initAppBox() {
		builder.labelStyle = { "min-width": "80px", "text-align": "right" };

		let compSet = builder.newCompSet({ title: "Application", clazzes: ["wkv-compset-border"], styleProps: { width: boxWidth } });
		this.viewWorkarea.prepend(compSet);

		//build controls
		let generalProps = { readOnly: true };
		this.name = builder.newTextComp({ label: "Name:" }, generalProps)
			.appendTo(compSet);

		this.version = builder.newTextComp({ label: "Version:" }, generalProps)
			.appendTo(compSet);

		this.description = builder.newTextAreaComp({ label: "Description:", rows: 3 }, generalProps)
			.appendTo(compSet)
			.style({ "align-items": "baseline" });
	}

	/**
	 */
	#initConfigBox() {
		//get a html coded config set element
		let compSet = this.getElement("server.config.set");
		CompBuilder.style(compSet, { "padding-top": "10px", width: boxWidth });

		//create command icons
		this.configBar = builder.newIconbarComp({
			icons: [
				{ name: "save", title: "Save current changes" },
				{ name: "redo", title: "Undo changes" }
			]
		}).prependTo(compSet)
			.style({ "margin-bottom": "10px", "font-size": "18px", "flex-direction": "row-reverse", "gap": "15px" })
			.config(comp => {
				comp.ctrls.forEach(ctrl => ctrl.classList.add("wkv-header-action-ctrl"));
				//save icon
				comp.ctrls[0].onclick = () => {
					updateInfos(getUpdateRequest(), (response) => {
						if (response?.status === "ok") {
							clearConfigChanges()
							console.log("App-Info update done");
						}
					});
				};
				//undo icon
				comp.ctrls[1].onclick = () => {
					//open confirmation dialog
					WbApp.confirm({
						title: "Confirmation required",
						message: "<b>Undo all changes</b><br>Do you want to discard all changes?"
					}, (value) => value ? clearConfigChanges(true) : null);
				};
			});

		this.setActionsEnabled([0, 1], false);

		//create a table
		this.configTable = new WorkViewTable(this.getElement("server.config"));
	}

	/**
	 */
	setActionsEnabled(idx, flag) {
		if (flag) {
			idx.forEach((i) => this.configBar.style(i, { "pointer-events": "all", color: "" }));
		} else {
			idx.forEach((i) => this.configBar.style(i, { "pointer-events": "none", color: "var(--border-gray)" }));
		}
	}

	/**
	 */
	writeDataToView() {
		if (this.needsDataReload) {
			//server data fetch call back
			getInfos((data) => {
				clearConfigChanges();

				//head data
				this.name.ctrl().value = data.name;
				this.version.ctrl().value = `${data.version} - Build [${data.buildDate} UTC]`;
				this.description.ctrl().value = data.description;

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
							if(typeUtil.isBooleanString(newValue) && !typeUtil.isBooleanString(orgCellValue)){
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

				this.needsDataReload = false;
			});
		}
	}
}

//export this view component as singleton instance
const viewInstance = new SystemInfoView("systemInfoView", "/jsmod/system-infos.html");
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
		callWebService("/api/system-infos").then((data) => {
			infoData = data;
			cb(infoData);
		});
	}
}

/**
 */
function updateInfos(request, cb) {
	//send changes to the server
	callWebService("/api/update-system-infos", JSON.stringify(request)).then((response) => {
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
	getView().setActionsEnabled([0, 1], false);
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
	getView().setActionsEnabled([0, 1], configChanges.size !== 0);
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
