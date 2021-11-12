package com.filipe.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.filipe.entity.User;

public interface UserRepository extends MongoRepository<User, String>{

	User findByEmail(String email);
	
	//User findByid(String id);
	
	
}
