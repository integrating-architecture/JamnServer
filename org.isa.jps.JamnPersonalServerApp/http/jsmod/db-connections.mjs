/* Authored by www.integrating-architecture.de */

import { WorkView, CompBuilder } from './view-classes.mjs';

let builder = new CompBuilder();

/**
 * An experimental WorkView created in javascript using a builder object.
 */
class DbConnectionsView extends WorkView {

	//demo functions to simulate behavior
	setTestResult = null;
	clearData = null;

	initialize() {
		super.initialize();
		this.setTitle("Database Connections");

		this.createUI();

		this.isInitialized = true;
	}

	writeDataToView(){
		this.clearData();
		this.setTestResult();
	}

	createUI() {
		builder.labelStyle = { "white-space": "nowrap" };

		let compSet = builder.newCompSet({ styleProps: { "margin-top": "10px" } });
		this.viewWorkarea.prepend(compSet);

		let lbWidth = "70px";
		let tfWidth = "150px";
		let fillerStyle = { "margin": "0px", "width": "20px", "text-align": "center" };

		//building control/components
		//in form of fieldset rows

		/**
		 * url component
		 */
		let dbUrl = builder.newTextDatalistComp({ label: "DB Url:", datalist: ["jdbc:oracle:thin", "jdbc.mysql"] })
			.appendTo(compSet)
			.style(0, { width: lbWidth })
			.style(1, { width: tfWidth })
			.attrb(1, { placeholder: "protocol" })
			.style({ "margin-bottom": "15px" })
			.addComp(
				builder.newTextComp({ label: "@" })
					.style(0, fillerStyle)
					.style(1, { width: "400px" })
					.attrb(1, { placeholder: "server:port/dbid" })
			)
			.config(comp => {
				//create new comp properties for data io
				comp.protocol = comp.ctrls[1];
				comp.server = comp.ctrls[3];
			});

		/**
		 * user component
		 */
		let dbUser = builder.newTextComp({ label: "User:" })
			.appendTo(compSet)
			.style(0, { width: lbWidth })
			.style(1, { width: tfWidth })
			.attrb(1, { placeholder: "name" })
			.addComp(
				builder.newTextComp()
					.style(0, fillerStyle)
					.style(1, { width: tfWidth })
					.attrb(1, { placeholder: "optional owner" })
			)
			.config(comp => {
				comp.userid = comp.ctrls[1];
				comp.owner = comp.ctrls[2];
			});


		/**
		 * password component and test 
		 */
		let testResult = null;
		let dbPwd = builder.newTextComp({ label: "Password:" })
			.appendTo(compSet)
			.style(0, { width: lbWidth })
			.style(1, { width: tfWidth })
			.attrb(1, { type: "password", placeholder: "********" })
			.addComp(
				builder.newButtonComp()
					.style(0, fillerStyle)
					.style(1, { width: "156px", height: "26px" })
					.config((comp) => {
						let pb = comp.ctrl();
						pb.title = "Test Connection";
						pb.value = "Test";
						pb.onclick = (evt) => {
							console.log("Test DB Connection");
							if(dbUser.userid.value.length > 0 ){
								let isOk = dbUser.userid.value === dbPwd.text.value;
								this.setTestResult(isOk, isOk ? "" : "Connection refused - invalid credentials - demo password must be user id");
							}
						};
					})
			)
			.addComp(
				testResult = builder.newTextAreaComp({ label: " ", rows: 1 }, { readOnly: true })
					.style(1, { "text-align": "left", "min-width": "80px", "width": "80px", border: "none", "min-height": "14px", resize: "none", overflow: "hidden" })
					.attrb(1, { placeholder: "<result>", title: "Test Result" })
					.config(comp => comp.text = comp.ctrl())
			)
			.config(comp => {
				comp.text = comp.ctrls[1];
			})
			.style({ "align-items": "baseline" });

		/**
		 * demo function to simulate connection test
		 */
		this.setTestResult = (status = -1, text = "") => {
			let okProps = { color: "green", resize: "none", width: "80px", height: testResult.text.style["min-height"] }
			if (status === false) {
				testResult.text.value = "FAILURE - " + text;
				testResult.style(1, { color: "red", resize: "auto", width: "550px" });
			} else if (status === true) {
				testResult.text.value = "Success";
				okProps.color = "green";
				testResult.style(1, okProps);
			} else {
				testResult.text.value = "";
				okProps.color = "";
				testResult.style(1, okProps);
			}
		};

		/**
		 * connections component and save button
		 * demo data 
		 */
		let connections = {
			"Oracle Test-Server": { protocol: "jdbc:oracle:thin", url: "TSRVORA:1521/XEPDB1", user: "admin" },
			"MySQL Dvlp-Server": { protocol: "jdbc.mysql", url: "DSRVMSQL:3306/dvlpdb1", user: "devel" }
		};
		let nameList = Object.getOwnPropertyNames(connections);
		let connectionNames = builder.newTextDatalistComp({ label: "Name:", datalist: nameList })
			.appendTo(compSet)
			.style(0, { width: lbWidth })
			.style(1, { width: tfWidth })
			.attrb(1, { placeholder: "connection name", title: "Empty to clear all fields" })
			.config((comp) => {
				comp.text = comp.ctrl();
				comp.ctrl().addEventListener('input', (evt) => {
					let key = evt.currentTarget.value;
					if (nameList.includes(key)) {
						dbUrl.protocol.value = connections[key].protocol;
						dbUrl.server.value = connections[key].url;
						dbUser.userid.value = connections[key].user;
						dbPwd.text.value = "";
					} else if (key === "") {
						this.clearData();
					}
				});
			})
			.addComp(
				builder.newButtonComp()
					.style(0, fillerStyle)
					.style(1, { width: "156px", height: "26px" })
					.config((comp) => {
						let pb = comp.ctrl();
						pb.title = "Save Connection";
						pb.value = "Save Connection";
						pb.onclick = (evt) => {
							console.log("Save Connection");
							this.setTestResult();
						};
					})
			)
			.style({ "margin-top": "15px" });

		this.clearData = ()=>{
			dbUrl.protocol.value = "";
			dbUrl.server.value = "";
			dbUser.userid.value = "";
			dbPwd.text.value = "";
			connectionNames.text.value = "";
		}
	}
}

//export this view component as singleton instance
const viewInstance = new DbConnectionsView("dbConnectionsView", "/jsmod/html-components/work-view.html");
export function getView() {
	return viewInstance;
}
