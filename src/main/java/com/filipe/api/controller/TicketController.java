package com.filipe.api.controller;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.filipe.api.dto.Summary;
import com.filipe.api.response.Response;
import com.filipe.entity.ChangeStatus;
import com.filipe.entity.Ticket;
import com.filipe.entity.User;
import com.filipe.enums.ProfileEnum;
import com.filipe.enums.StatusEnum;
import com.filipe.security.jwt.JwtTokenUtil;
import com.filipe.service.TicketService;
import com.filipe.service.UserService;

@RestController
@RequestMapping("/api/ticket")
@CrossOrigin(origins = "*")
public class TicketController {

	@Autowired
	private TicketService ticketService;

	@Autowired
	protected JwtTokenUtil jwtTokenUtil;

	@Autowired
	private UserService userService;

	// CREATE A TICKET

	@PostMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> create(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {

		Response<Ticket> response = new Response<Ticket>();

		try {
			validateCreateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErros().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			ticket.setStatus(StatusEnum.getStatus("New"));
			ticket.setUser(userFromRequest(request));
			ticket.setDate(new Date());
			ticket.setNumber(generateNumber());

			Ticket ticketPersisted = ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);

		} catch (Exception e) {
			response.getErros().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);

	}

	// UPDATE THE TICKET

	@PutMapping
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<Ticket>> update(HttpServletRequest request, @RequestBody Ticket ticket,
			BindingResult result) {

		Response<Ticket> response = new Response<>();

		try {
			validateUpdateTicket(ticket, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErros().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}

			Optional<Ticket> ticketCurrent = ticketService.findById(ticket.getId());
			ticket.setStatus(ticketCurrent.get().getStatus());
			ticket.setUser(ticketCurrent.get().getUser());
			ticket.setDate(ticketCurrent.get().getDate());
			if (ticketCurrent.get().getAssignedUser() != null) {
				ticket.setAssignedUser(ticketCurrent.get().getAssignedUser());
			}

			Ticket ticketPersisted = ticketService.createOrUpdate(ticket);
			response.setData(ticketPersisted);

		} catch (Exception e) {
			response.getErros().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);
	}

	// FIND THE TICKET BY ID

	@GetMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Optional<Ticket>>> findById(@PathVariable("id") String id) {
		Response<Optional<Ticket>> response = new Response<>();

		Optional<Ticket> ticket = ticketService.findById(id);
		if (ticket == null) {
			response.getErros().add("Register not found: " + id);
			return ResponseEntity.badRequest().body(response);

		}
		// Buscar a lista de alterações de status do ticket
		List<ChangeStatus> changes = new ArrayList<ChangeStatus>();
		Iterable<ChangeStatus> changesCurrent = ticketService.listChangeStatus(ticket.get().getId());
		for (Iterator<ChangeStatus> iterator = changesCurrent.iterator(); iterator.hasNext();) {
			ChangeStatus changeStatus = (ChangeStatus) iterator.next();
			changeStatus.setTicket(null);
			changes.add(changeStatus);
		}
		ticket.orElse(null).setChanges(changes);
		response.setData(ticket);
		return ResponseEntity.ok(response);
	}

	// DELET THE TICKET
	@DeleteMapping(value = "{id}")
	@PreAuthorize("hasAnyRole('CUSTOMER')")
	public ResponseEntity<Response<String>> delete(@PathVariable("id") String id) {

		Response<String> response = new Response<String>();
		Optional<Ticket> ticket = ticketService.findById(id);
		if (ticket.isEmpty()) {
			response.getErros().add("Register not found: " + id);
			return ResponseEntity.badRequest().body(response);

		}
		ticketService.delete(id);
		return ResponseEntity.ok(new Response<String>());

	}

	@GetMapping(value = "{page}/{count}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findAll(HttpServletRequest request, @PathVariable("page") int page,
			@PathVariable("count") int count) {

		Response<Page<Ticket>> response = new Response<Page<Ticket>>();

		Page<Ticket> tickets = null;

		User userRequest = userFromRequest(request);

		if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {

			tickets = ticketService.listTicket(page, count);

		} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {

			tickets = ticketService.findByCurrentUser(page, count, userRequest.getId());
		}
		response.setData(tickets);

		return ResponseEntity.ok(response);

	}

	// Find by Parameters
	@GetMapping(value = "{page}/{count}/{number}/{title}/{status}/{priority}/{assigned}/")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Page<Ticket>>> findByParameters(HttpServletRequest request,	@PathVariable("page") int page, 
			@PathVariable("count") int count,
			@PathVariable("number") Integer number,
			@PathVariable("title") String title,
			@PathVariable("status") String status,
			@PathVariable("priority") String priority, 
			@PathVariable("assigned") boolean assigned) {

		title = title.equals("uniformed") ? "" : title;
		status = status.equals("uniformed") ? "" : status;
		priority = priority.equals("uniformed") ? "" : priority;

		Response<Page<Ticket>> response = new Response<Page<Ticket>>();
		Page<Ticket> tickets = null;
		if (number > 0) {
			tickets = ticketService.findByNumber(page, count, number);
		} else {
			User userRequest = userFromRequest(request);
			if (userRequest.getProfile().equals(ProfileEnum.ROLE_TECHNICIAN)) {
				if (assigned) {
					tickets = ticketService.findByParametersAndAssignedUser(page, count, title, status, priority,
							userRequest.getId());
				} else {
					tickets = ticketService.findByParameters(page, count, title, status, priority);
				}

			} else if (userRequest.getProfile().equals(ProfileEnum.ROLE_CUSTOMER)) {
				tickets = ticketService.findByParametersAndAssignedUser(page, count, title, status, priority, userRequest.getId());
			}
		}
		
		response.setData(tickets);
		return ResponseEntity.ok(response);
	}
	
	
	//Change the ticket status
	@PutMapping(value = "{id}/{status}")
	@PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN')")
	public ResponseEntity<Response<Ticket>> changeStatus(@PathVariable("id") String id,
													     @PathVariable("status") String status,
													     HttpServletRequest request,
													     @RequestBody Ticket ticket,
													     BindingResult result){
		
		Response<Ticket> response = new Response<Ticket>();
		
		try {
			validateChangeStatus(id, status, result);
			if (result.hasErrors()) {
				result.getAllErrors().forEach(error -> response.getErros().add(error.getDefaultMessage()));
				return ResponseEntity.badRequest().body(response);
			}
			
			Optional<Ticket> ticketCurrent = ticketService.findById(id);
			ticketCurrent.orElseThrow().setStatus(StatusEnum.getStatus(status));
			if(status.equals("Assigned")) {
				ticketCurrent.orElseThrow().setAssignedUser(userFromRequest(request));
			}

			Ticket ticketPersisted =  ticketService.createOrUpdate(ticketCurrent.get());
			
			ChangeStatus changeStatus = new ChangeStatus();
			changeStatus.setUserChange(userFromRequest(request));
			changeStatus.setDateChangeStatus(new Date());
			changeStatus.setStatus(StatusEnum.getStatus(status));
			changeStatus.setTicket(ticketPersisted);
			
			ticketService.createChangeStatus(changeStatus);
			
			response.setData(ticketPersisted);
			
		} catch (Exception e) {
			response.getErros().add(e.getMessage());
			return ResponseEntity.badRequest().body(response);
		}
		return ResponseEntity.ok(response);
		
	}
	
	//Method to show the ticket resume - new, approved, repproved etc
	@GetMapping(value = ("/summary"))
	public ResponseEntity<Response<Summary>> findSumary(){
		
		Response<Summary> response = new Response<Summary>();
		Summary summary = new Summary();
		int amountNew = 0;
		int amountResolved = 0;
		int amountApproved = 0;
		int amountDisapproved = 0;
		int amountAssigned = 0;
		int amountClosed = 0;
		
		Iterable<Ticket> tickets = ticketService.findAll();
		if (tickets !=null) {
			
			for (Iterator<Ticket> iterator = tickets.iterator(); iterator.hasNext();) {
				Ticket ticket = (Ticket) iterator.next();
				if(ticket.getStatus().equals(StatusEnum.New)) {
					amountNew++;
				}
				if(ticket.getStatus().equals(StatusEnum.Resolved)) {
					amountResolved++;
				}
				if(ticket.getStatus().equals(StatusEnum.Approved)) {
					amountApproved++;
				}
				if(ticket.getStatus().equals(StatusEnum.Disapproved)) {
					amountDisapproved++;
				}
				if(ticket.getStatus().equals(StatusEnum.Assigned)) {
					amountAssigned++;
				}
				if(ticket.getStatus().equals(StatusEnum.Closed)) {
					amountClosed++;
				}
			}
			summary.setAmountNew(amountNew);
			summary.setAmountResolved(amountResolved);
			summary.setAmountApproved(amountApproved);
			summary.setAmountDisapproved(amountDisapproved);
			summary.setAmountAssigned(amountAssigned);
			summary.setAmountClosed(amountClosed);
			
		}
		
		response.setData(summary);
		
		return ResponseEntity.ok(response);
		
	}
	

	private Integer generateNumber() {
		Random random = new Random();
		return random.nextInt(9999);
	}

	public User userFromRequest(HttpServletRequest request) {
		String token = request.getHeader("Authorization");
		String email = jwtTokenUtil.getUserNameFromToken(token);
		return userService.findByEmail(email);
	}

	private void validateCreateTicket(Ticket ticket, BindingResult result) {

		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title not information"));
			return;

		}
	}

	private void validateUpdateTicket(Ticket ticket, BindingResult result) {

		if (ticket.getId() == null) {
			result.addError(new ObjectError("Ticket", "Title not information"));
			return;

		}

		if (ticket.getTitle() == null) {
			result.addError(new ObjectError("Ticket", "Title not information"));
			return;

		}
	}
	
	private void validateChangeStatus(String id, String status, BindingResult result) {

		if (id == null || id.equals("")) {
			result.addError(new ObjectError("Ticket", "Id not informed"));
			return;

		}

		if (status == null || status.equals("")) {
			result.addError(new ObjectError("Ticket", "Status not informed"));
			return;

		}
	}

}
