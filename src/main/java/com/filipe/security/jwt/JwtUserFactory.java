package com.filipe.security.jwt;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.filipe.entity.User;
import com.filipe.enums.ProfileEnum;

public class JwtUserFactory  {
	
	private JwtUserFactory() {
		
	}
	
	//Converte um JwtUser com base nos dados do usuário
	public static JwtUser create(User user) {
		return new JwtUser(user.getId(), user.getEmail(), user.getPassword(),
				mapToGrantedAuthorities(user.getProfile()));
		
	}

	//Converte o perfil do usuário para o formato utilizado no Spring security
	private static List<GrantedAuthority> mapToGrantedAuthorities(ProfileEnum profileEnum) {
		List<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		authorities.add(new SimpleGrantedAuthority(profileEnum.toString()));
		return authorities;
	}
	

}
