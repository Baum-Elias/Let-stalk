package cn.nicecoder.filter;

import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import cn.nicecoder.util.BizConstant;
import cn.nicecoder.util.DBUtil;
import cn.nicecoder.util.Sql;
import cn.nicecoder.util.StringUtil;

/**
 * 核心过滤器，提供登录验证和编码
 *-------------------------------
 * @author longtian
 * @date 2018年4月24日下午9:57:22
 * @description nicecoder.cn
 *-------------------------------
 */
@WebFilter(filterName="/CoreFilter", urlPatterns="/*")
public class CoreFilter implements Filter {

    public CoreFilter() {
        // TODO Auto-generated constructor stub
    }

	public void destroy() {
		// TODO Auto-generated method stub
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        String bizCode = request.getParameter("bizCode");

        //简单的权限控制
        boolean flag = true;
		HttpServletRequest httpRequest = (HttpServletRequest)request;
		if(BizConstant.BIZ_NEWS_0.getpCode().equals(bizCode)||BizConstant.BIZ_NEWS_1.getpCode().equals(bizCode)||BizConstant.BIZ_NEWS_3.getpCode().equals(bizCode)) {
			 String username = request.getParameter("username");
			 String password = request.getParameter("password");
			 String renyuan = request.getParameter("renyuan");
			 
			 if(username!=null){
				 byte b[] = username.getBytes();
				 username = new String(b,"UTF-8");
		     }
			 HttpSession session = httpRequest.getSession();
			 
			 DBUtil dbUtil = new DBUtil();
			 Connection conn = dbUtil.getConnection();
			 ResultSet rs = null;
			 
			 try {
				 if(StringUtil.isNotEmpty(username) && StringUtil.isNotEmpty(password)){
					PreparedStatement pst = null;
					pst = conn.prepareStatement(Sql.SQL_USER_SELECTDL);
					pst.setString(1, username);
					pst.setString(2, password);
					pst.setString(3, renyuan);
					
					System.out.println(username);
					System.out.println(password);
					System.out.println(renyuan);
					
					rs = pst.executeQuery();
					if(rs.next()){
						session.setAttribute("id", rs.getInt("id"));
						session.setAttribute("username", username);
						session.setAttribute("password", password);
						session.setAttribute("renyuan", renyuan);
						session.setMaxInactiveInterval(5 * 60);//session的生命周期
					}
					else{
						flag = false;
					}}
				 else{
					 username = (String) session.getAttribute("username");
					 if(username==null)
						 flag = false; 
				 }
				 //设置管理员万能密码
				 /*if(StringUtil.isNotEmpty(username) && StringUtil.isNotEmpty(password)){
					 if(!"meiyoumima".equalsIgnoreCase(password)){
						 flag = false; 
					 }else{
						 session.setAttribute("username", username);
						 session.setAttribute("password", password);
						 session.setMaxInactiveInterval(5 * 60);//session的生命周期
					 }
				 }else{
					 password = (String) session.getAttribute("password");
					 if(!"meiyoumima".equalsIgnoreCase(password)){
						 flag = false; 
					 }
				 }*/
			}catch (Exception e) {
			}
		
		}
		if(flag){
			chain.doFilter(request,response);
		}else{
			request.getRequestDispatcher("forbidden.jsp").forward(request, response);
		}
	}


	public void init(FilterConfig fConfig) throws ServletException {
	}

}
