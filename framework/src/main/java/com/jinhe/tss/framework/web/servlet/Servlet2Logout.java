package com.jinhe.tss.framework.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jinhe.tss.framework.sso.Environment;
import com.jinhe.tss.framework.sso.online.OnlineUserManagerFactory;
import com.jinhe.tss.framework.web.dispaly.SuccessMessageEncoder;

/**
 * <p> 
 * 注销登录的Servlet。 
 * </p>
 */
@WebServlet(name="LogoutServlet", urlPatterns="/logout.in")
public class Servlet2Logout extends HttpServlet {

    private static final long serialVersionUID = 807941967787932751L;
 
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	request.getSession().invalidate(); // 销毁session 
    	
    	Long userId = Environment.getUserId();
    	if(userId != null) {
    		OnlineUserManagerFactory.getManager().logout(userId);
    	}
        
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print( new SuccessMessageEncoder("注销成功", SuccessMessageEncoder.NO_POPUP_TYPE).toXml());
        out.flush();
        out.close();
    }
}
