package com.filipe.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.filipe.enums.ProfileEnum;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Document
public class User {

	@Id
	private String id;
	
	@Indexed(unique = true)
	@NotNull(message ="Email required")
	private String email;
	
	@Size(min = 6, message = "Min length to password is 6")
	@NotNull(message=" Password required")
	private String password;
	
	private ProfileEnum profile;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public ProfileEnum getProfile() {
		return profile;
	}

	public void setProfile(ProfileEnum profile) {
		this.profile = profile;
	}
	
	
	
}
