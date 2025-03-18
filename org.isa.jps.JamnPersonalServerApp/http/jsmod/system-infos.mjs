/* Authored by www.integrating-architecture.de */

import { callWebService } from '../jsmod/tools.mjs';
import { WorkView, WorkViewTable, TableData } from '../jsmod/view-classes.mjs';

/**
 * Concrete view class for this info component
 */
class SystemInfoView extends WorkView {

	name;
	version;
	description;
	configTable;

	initialize() {
		super.initialize();
		this.setTitle("System Infos");

		this.name = this.getElement("server.name");
		this.version = this.getElement("server.version");
		this.description = this.getElement("server.description");

		this.configTable = new WorkViewTable(this.getElement("server.config"));   

		this.isInitialized = true;
	}

	writeDataToView() {
		getInfos((data) => {
			this.name.innerHTML = data.name;
			this.version.innerHTML = data.version;
			this.description.innerHTML = data.description;

			let tableData = new TableData();
			// "data.config" has the structure: { name1:value1, name2:value2 ... }
			// create a 2 column tableData from it
			let names = Object.getOwnPropertyNames(data.config);
			names.sort((a, b) => a.localeCompare(b));
	
			names.forEach((name) => {
				let row = new Map();
				row.set(name, name);
				row.set("val:"+name, data.config[name]);
				tableData.addRow(name, row);
			})
	
			tableData.cellClick = (rowKey, colKey, evt) => {
				let cols = tableData.rows.get(rowKey);
				let colValue = cols.get(colKey);
				console.log(colValue);
			};
			this.configTable.setData(tableData);

			this.configTable.getHeader(0).getElementsByTagName("i")[0].onclick = (evt) => {
				this.configTable.sortByColumn(0);
				this.configTable.toggleColSort(0);
			};

			this.configTable.getHeader(0).getElementsByTagName("input")[0].onkeyup = (evt) => {
				this.configTable.filterRows(0, evt.target.value);
			};

		});
	}
}

//export this view component as singleton instance
const viewInstance = new SystemInfoView("systemInfoView", "/jsmod/system-infos.html");
export function getView() {
	return viewInstance;
}

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
