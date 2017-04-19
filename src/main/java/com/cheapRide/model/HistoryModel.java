package com.cheapRide.model;
/**
 * 
 * @author Agnes 
 *
 */

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "history")
public class historyModel {
@Id
private String date;
private String provider;
private String pickup;
private String destination;
private String fee;
private String type;

public String getDate(){
	return date;
}
public void setDate(String date){
		this.date = date;
}
public String getProvider(){
	return provider;
}
public void setProvider(String provider){
	this.provider = provider;
}
public String getPickup(){
	return pickup;
}
public void setPickup(String pickup){
	this.pickup = pickup;
}
public String getDestination(){
	return destination;
}
public void setDestination(String destination){
	this.destination = destination;
}
public String getFee(){
	return fee;
}
public void setFee(String fee){
	this.fee = fee;
}
public String getType(){
	return type;
}
public void setType(String type){
	this.type = type;
}
}