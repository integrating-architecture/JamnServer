/* Authored by www.integrating-architecture.de */

import { WorkView, ViewBuilder } from './view-classes.mjs';
import { callWebService } from '../jsmod/tools.mjs';
import * as webapi from '../jsmod/webapi.mjs';

/**
 * A Database connection WorkView created in javascript using a builder object.
 */
class DbConnectionsView extends WorkView {

	elems = {};
	uiobj = {};

	connections = null;
	currentConnection;

	initialize() {
		super.initialize();
		this.setTitle("Database Connections");

		this.getConnections(() => {
			this.extendViewMenu();
			this.createUI();
			this.setVisible(true);
			this.isInitialized = true;
		})
	}

	getConnections(cb) {
		if (this.connections) {
			cb(this.connections);
		} else {
			//load the data from server
			callWebService(webapi.service_get_dbconnections).then((response) => {
				//connections are sent as an array - create an object from it
				this.connections = {};
				response.connections.forEach((item) => this.connections[item.name] = item);
				cb(this.connections);
			});
		}
	}

	extendViewMenu() {
		if (this.headerMenu) {
			this.headerMenu.addItem("Clear View", (evt) => {
				this.clearData();
			}, { separator: "top" });
		}
	}

	createUI() {
		let builder = new ViewBuilder();
		builder.setElementCollection(this.elems);
		builder.setObjectCollection(this.uiobj);

		//define some style defaults
		builder.defaultStyles.comp = { "margin-bottom": "0" };
		builder.defaultStyles.label = { "width": "70px" };
		builder.defaultStyles.button = { "width": "156px", "height": "26px" };
		builder.defaultStyles.textField = { "width": "150px" };
		let hgap = "20px";

		//create the container
		let compSet = builder.newFieldset({ styleProps: { "margin-top": "10px", "gap": "10px" } });
		this.viewWorkarea.prepend(compSet);

		//build the controls
		let connectionNames = Object.getOwnPropertyNames(this.connections);
		builder.newViewComp()
			.style({ "margin-bottom": "10px" })
			.addLabelTextField(
				{ text: "Name:" },
				{ varid: "tfConnectionName", datalist: connectionNames, attribProps: { placeholder: "connection name" } },
				(target) => {
					target.textfield.style.width = "250px";
					target.textfield.addEventListener('input', (evt) => {
						this.changeCurrentConnection(evt.currentTarget.value);
					})
				})
			.addActionIcon({ varid: "icoErase", iconName: "eraser", title: "Clear current selection", styleProps: { "margin-left": "10px" } }, (target) => {
				target.icon.onclick = () => {
					this.clearData();
				}
			})
			.addActionIcon({ varid: "icoSave", iconName: "save", title: "Save current connection", styleProps: { "margin-left": "20px" } }, (target) => {
				target.icon.onclick = () => {
					this.saveConnection();
				}
			})
			.addContainer({ styleProps: { width: "20px", height: "20px", "margin-right": "20px", "border-right": "1px solid var(--border-gray)" } })
			.addActionIcon({ varid: "icoDelete", iconName: "trash", title: "Delete current connection" }, (target) => {
				target.icon.onclick = () => {
					this.deleteConnection();
				}
			})
			.appendTo(compSet);

		let propsBox = builder.newFieldset({ title: "Connection properties", clazzes: ["wkv-compset-border"], styleProps: { width: "700px", "row-gap": "10px", "margin-bottom": "0px" } });
		compSet.append(propsBox);

		builder.newViewComp()
			.addLabelTextField(
				{ text: "DB Url:" },
				{ varid: "tfDbUrl", styleProps: { width: "600px" }, attribProps: { placeholder: "url like e.g. - jdbc:oracle:thin:@localhost:1521/XEPDB1", "data-bind": "url" } })
			.appendTo(propsBox);

		builder.newViewComp()
			.addLabelTextField(
				{ text: "User:" },
				{ varid: "tfUser", attribProps: { placeholder: "name", "data-bind": "user" } })
			.addTextField(
				{ varid: "tfOwner", attribProps: { placeholder: "optional owner", "data-bind": "owner" }, styleProps: { "margin-left": hgap } })
			.appendTo(propsBox);

		builder.newViewComp()
			.style({ "align-items": "baseline" })
			.addLabelTextField(
				{ text: "Password:" },
				{ varid: "tfPwd", attribProps: { type: "password", placeholder: "********" } })
			.addButton(
				{ text: "Test", title: "Test connection", varid: "pbTest", styleProps: { "margin-left": hgap } },
				(target) => {
					target.button.onclick = (evt) => {
						this.runTestDbConnection();
					};
				})
			.addTextArea(
				{ varid: "tfTestResult", rows: "1", readOnly: true, attribProps: { placeholder: "<result>", title: "Test Result" }, styleProps: { "overflow": "hidden", "margin-left": hgap, "text-align": "left", "min-width": "80px", "width": "80px", "min-height": "14px" } })
			.appendTo(propsBox);

	}

	changeCurrentConnection(key) {
		if (this.connections.hasOwnProperty(key)) {
			this.currentConnection = this.connections[key];
			this.writeDataToView();
		} else {
			this.currentConnection = null;
		}
	}

	clearData(excludes = []) {
		let names = Object.getOwnPropertyNames(this.elems);
		names.forEach((name) => {
			let ctrl = this.elems[name];
			if (!excludes.includes(ctrl)) {
				ViewBuilder.clearControl(ctrl);
			}
		});

		let key = this.elems.tfConnectionName.value.trim();
		if (key !== "" && this.connections.hasOwnProperty(key)) {
			this.currentConnection = this.connections[key];
		} else {
			this.currentConnection = null;
		}

		this.showConnectionTestResult();
	}

	writeDataToView() {
		let excludes = [this.elems.tfConnectionName];
		let bindings = this.uiobj.bindings;

		if (this.currentConnection) {
			let names = Object.getOwnPropertyNames(bindings);
			names.forEach((name) => {
				let ctrl = bindings[name];
				ctrl.value = this.currentConnection[name];
				excludes.push(ctrl);
			});
			this.clearData(excludes);
		} else {
			this.clearData();
		}
	}

	saveConnection() {
		if (this.currentConnection) {
			let request = JSON.stringify({ connections: [this.currentConnection] });
			callWebService(webapi.service_save_dbconnections, request).then((response) => {
				if (response.status === "ok") {
					console.log("Saved: ok");
				}
			});
		}
	}

	deleteConnection() {
		if (this.currentConnection) {
			WbApp.confirm({
				message: `<b>Delete item</b><br>Do you want to delete connection <b>[${this.currentConnection.name}]</b> ?`
			}, (val) => {
				if (val) {
					let request = JSON.stringify({ connections: [this.currentConnection] });
					callWebService(webapi.service_delete_dbconnections, request).then((response) => {
						if (response.status === "ok") {
							console.log("Deleted: ok");
						}
					});
				}
			});
		}
	}

	runTestDbConnection() {
		let userId = this.elems.tfUser.value.trim();
		let pwd = this.elems.tfPwd.value.trim();
		if (userId) {
			if (userId == pwd) {
				this.showConnectionTestResult(true);
			} else {
				this.showConnectionTestResult(false, "Connection refused - invalid credentials - demo password must be = user id");
			}
		} else {
			this.showConnectionTestResult();
		}
	}

	showConnectionTestResult(status = -1, text = "") {
		let ctrl = this.elems["tfTestResult"];
		let okProps = { color: "green", resize: "none", width: "80px", height: ctrl.style["min-height"] };

		if (status === false) {
			ctrl.value = "FAILURE - " + text + "\n\n" + new Error().stack;
			ViewBuilder.setStyleOf(ctrl, { color: "red", resize: "auto", width: "550px" });
		} else if (status === true) {
			ctrl.value = "Success";
			ViewBuilder.setStyleOf(ctrl, okProps);
		} else {
			ctrl.value = "";
			okProps.color = "";
			ViewBuilder.setStyleOf(ctrl, okProps);
		}
	}
}

//export this view component as singleton instance
const viewInstance = new DbConnectionsView("dbConnectionsView", "/jsmod/html-components/work-view-tmpl.html");
export function getView() {
	return viewInstance;
}
