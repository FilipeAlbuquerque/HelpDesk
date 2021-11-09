package com.filipe.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.filipe.entity.User;
import com.filipe.security.jwt.JwtUserFactory;
import com.filipe.service.UserService;

@Service
public class JwtUserDetailsServiceImpl implements UserDetailsService {
	
	@Autowired
	private UserService userService;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User user = userService.findByEmail(email);
		
		if (user == null) {
			throw new UsernameNotFoundException(String.format("No user found with '%s'.", email));
		} else {
			
			return JwtUserFactory.create(user);
		}

	}

}
