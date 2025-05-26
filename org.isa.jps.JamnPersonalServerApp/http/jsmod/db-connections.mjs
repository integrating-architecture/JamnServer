/* Authored by www.integrating-architecture.de */

import { WorkView, ViewBuilder } from './view-classes.mjs';

/**
 * An experimental WorkView created in javascript using a builder object.
 */
class DbConnectionsView extends WorkView {

	elems = {};
	uiobj = {};

	protocols;
	connections;
	currentConnection;
	tfConnectionName;

	initialize() {
		super.initialize();
		this.setTitle("Database Connections");

		this.extendViewMenu();
		this.createDemoData();
		this.createUI();

		this.isInitialized = true;
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
		builder.defaultStyles.button = { "width": "156px", height: "26px" };
		builder.defaultStyles.textField = { "width": "150px" };
		let hgap = "20px";

		//create the container
		let compSet = builder.newFieldset({ styleProps: { "margin-top": "10px", "gap": "10px" } });
		this.viewWorkarea.prepend(compSet);

		//build the controls
		builder.newViewComp()
			.addLabelTextField(
				{ text: "DB Url:" },
				{ varid: "tfUrlProtocol", datalist: this.protocols, attribProps: { placeholder: "protocol", "data-bind": "protocol" } })
			.addLabelTextField(
				{ text: "@", styleProps: { width: hgap, "margin": "0", "text-align": "center" } },
				{ varid: "tfUrlServer", styleProps: { width: "400px" }, attribProps: { placeholder: "server:port/dbid", "data-bind": "url" } })
			.appendTo(compSet);

		builder.newViewComp()
			.addLabelTextField(
				{ text: "User:" },
				{ varid: "tfUser", attribProps: { placeholder: "name", "data-bind": "user" } })
			.addTextField(
				{ varid: "tfOwner", attribProps: { placeholder: "optional owner", "data-bind": "owner" }, styleProps: { "margin-left": hgap } })
			.appendTo(compSet);

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
			.appendTo(compSet);

		let connectionNames = Object.getOwnPropertyNames(this.connections);
		builder.newViewComp()
			.addLabelTextField(
				{ text: "Name:" },
				{ varid: "tfConnectionName", datalist: connectionNames, attribProps: { placeholder: "connection name" } },
				(target)=>{
					this.tfConnectionName = target.textfield;
					target.textfield.addEventListener('input', (evt) => {
						this.changeCurrentConnection(evt.currentTarget.value);
					})
				})
			.addButton(
				{ text: "Save Connection", title: "Save Connection", varid: "pbSave", styleProps: { "margin-left": hgap } })
			.appendTo(compSet);
	}

	createDemoData() {
		this.protocols = ["jdbc.oracle.thin", "jdbc.mysql"];
		this.connections = {
			"Oracle Test-Server": { protocol: "jdbc.oracle.thin", url: "TSORA:1521/XEPDB1", user: "admin", owner: "" },
			"MySQL Dvlp-Server": { protocol: "jdbc.mysql", url: "DSMSQL:3306/dvlpdb1", user: "devel", owner: "" }
		};
	}

	changeCurrentConnection(key){
		if(this.connections.hasOwnProperty(key)){
			this.currentConnection = this.connections[key];
			this.writeDataToView();
		}else{
			this.currentConnection = null;
		}
	}

	clearData(excludes=[]) {
		this.showConnectionTestResult();
		let names = Object.getOwnPropertyNames(this.elems);
		names.forEach((name) => {
			let ctrl = this.elems[name];
			if(!excludes.includes(ctrl)){
				ViewBuilder.clearControl(ctrl);
			}
		});
	}

	writeDataToView() {
		let excludes = [this.tfConnectionName];
		let bindings = this.uiobj.bindings;

		if(this.currentConnection){
			let names = Object.getOwnPropertyNames(bindings);
			names.forEach((name) => {
				let ctrl = bindings[name];
				ctrl.value = this.currentConnection[name];
				excludes.push(ctrl);
			});
			this.clearData(excludes);
		}else{
			this.clearData();
		}
	}

	readDataFromView() {
		//to be overwritten
	}

	runTestDbConnection() {
		let userId = this.elems.tfUser.value.trim();
		let pwd = this.elems.tfPwd.value.trim();
		if (userId && pwd) {
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
