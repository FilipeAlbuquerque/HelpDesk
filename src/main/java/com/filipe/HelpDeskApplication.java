package com.filipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelpDeskApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelpDeskApplication.class, args);
	}
//	
//	
//	@Bean
//	CommandLineRunner init(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//		
//		return args -> {
//			initUsers(userRepository, passwordEncoder);
//		};
//		
//	}
//	
//	private void initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
//		
//		User admin = new User();
//		admin.setEmail("admin@helpdesk.com");
//		admin.setPassword(passwordEncoder.encode("123456"));
//		admin.setProfile(ProfileEnum.ROLE_ADMIN);
//		
//		
//		User find = userRepository.findByEmail("admin@helpdesk.com");
//		if (find == null) {
//			
//			userRepository.save(admin);
//		}
//		
//	}

}
