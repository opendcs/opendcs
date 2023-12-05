package portal.decodes;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Error extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // reading the user input

        String destination = "/error.jsp";

        RequestDispatcher rd = getServletContext().getRequestDispatcher(destination);
        rd.forward(request, response);           
    }
}
