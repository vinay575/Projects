package com.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.entity.BankDTO;
import com.entity.StatementDTO;
import com.dao.UserDAO;
import com.entity.UserDTO;

@Controller
public class PageController {

	UserDAO userDAO = new UserDAO();
	ModelAndView mv = new ModelAndView();

	@GetMapping("/Registration")
	public ModelAndView getRegistrationPage() {

		mv.setViewName("Registration");
		return mv;
	}

	@PostMapping("/Registration")
	public String getUserDetails(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String username = request.getParameter("name");
		String phoneNo = request.getParameter("phoneNo");
		String userAddress = request.getParameter("address");
		String email = request.getParameter("email");
		String password = request.getParameter("password");
	

		UserDTO user = new UserDTO(username, phoneNo, userAddress, email, password);
		if (userDAO.saveUserDetails(user)) {

			return "redirect:/Login";
		} else {
			return "redirect:/Registration";
		}
	}

	@GetMapping("/Login")
	public String showLoginForm() {
		return "Login";
	}
	@PostMapping("/Login")
	public String processLogin(@RequestParam("username") String username, @RequestParam("password") String password,
	                           HttpSession session, Model model) {

	    UserDTO user = userDAO.showUserDetails(username, password);
	    if (user != null) {
	        List<BankDTO> bank = userDAO.getBankDetailsByUserId(user.getUser_id());
	        session.setAttribute("user", user);
	        model.addAttribute("banklist", bank);
	        model.addAttribute("success", "Login successful.");
	        return "Welcome";
	    } else {
	        model.addAttribute("error", "Invalid username or password");
	        return "Login";
	    }
	}

	@GetMapping("/AddAccount")
	public ModelAndView getAddBankPage() {

		mv.setViewName("AddAccount");
		return mv;
	}

	@PostMapping("/AddAccount")
	public ModelAndView processAddAccount(HttpServletRequest request, HttpServletResponse response, Model model) {

	    String userId_string = request.getParameter("userId");
	    int userId = Integer.parseInt(userId_string);
	    String bankAccountNo = request.getParameter("accountNumber");
	    String bankName = request.getParameter("bankName");
	    String ifscCode = request.getParameter("ifscCode");
	    String accountType = request.getParameter("accountType");
	    String money = request.getParameter("initialBalance");
	    double currentBalance = Double.parseDouble(money);

	    BankDTO bank = new BankDTO(userId, bankAccountNo, bankName, ifscCode, accountType, currentBalance);
	    if (userDAO.saveBankDetails(bank)) {
	        HttpSession session = request.getSession();
	        UserDTO userDTO = (UserDTO) session.getAttribute("user");
	        List<BankDTO> Bank = userDAO.getBankDetailsByUserId(userDTO.getUser_id());
	        model.addAttribute("banklist", Bank);
	        mv.setViewName("Welcome");

	    } else {
	        model.addAttribute("error", "something went wrong");
	        mv.setViewName("AddAccount");
	    }

	    return mv;
	}

	@PostMapping("/Process")
	public ModelAndView processButton(@RequestParam("selectedAction") String selectedAction,
	                                   @RequestParam("selectedAccount") int accountID,
	                                   HttpSession session) {
	    ModelAndView modelAndView = new ModelAndView();

	    // Pass the selected account ID to the session
	    session.setAttribute("selectedAccountID", accountID);

	    if ("Statement".equals(selectedAction)) {
	        modelAndView.setViewName("redirect:/Statement");
	    } else if ("AddMoney".equals(selectedAction)) {
	        modelAndView.setViewName("AddMoney"); 
	    } else if ("SendMoney".equals(selectedAction)) {
	        modelAndView.setViewName("SendMoney"); 
	    } else {
	        modelAndView.setViewName("Login"); 
	    }

	    return modelAndView;
	}

	
			
    @PostMapping("/AddMoney")
    public ModelAndView processAddMoney(@RequestParam("accountID") int accountID,
                                       @RequestParam("amount") double amount,
                                       HttpSession session) {
        Date currentDate = new Date(System.currentTimeMillis());

        // Retrieve the BankDTO object for the given account ID
        BankDTO bank = userDAO.getBankById(accountID);

        // Update the balance of the BankDTO object
        bank.setCurrentBalance(bank.getCurrentBalance() + amount);

        // Log the transaction
        StatementDTO transaction = new StatementDTO(accountID, accountID, amount, "add", currentDate);
        userDAO.logTransaction(transaction);

        // Save the updated BankDTO object back to the database
        userDAO.updateBalance(bank);

        // Re-fetch the updated bank details and store them in the session
        UserDTO user = (UserDTO) session.getAttribute("user");
        List<BankDTO> Bank = userDAO.getBankDetailsByUserId(user.getUser_id());
        session.setAttribute("banklist", Bank);

        mv.addObject("success", "succesfully added money");
        mv.setViewName("Welcome");

        return mv;
    }
    
    
    @GetMapping("/Statement")
    public String showStatementPage(Model model, HttpSession session) {
        // Retrieve the selected account ID from the session
        int selectedAccountID = (int) session.getAttribute("selectedAccountID");

        // Fetch statements related to the selected account ID
        List<StatementDTO> statementList = userDAO.getStatementsByAccountID(selectedAccountID);

        // Add the statement list to the model for rendering in the Statement.jsp page
        model.addAttribute("statementList", statementList);

        // Return the name of the Statement.jsp view
        return "Statement";
    }

}







