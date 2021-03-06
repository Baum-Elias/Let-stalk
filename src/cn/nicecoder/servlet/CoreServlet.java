package cn.nicecoder.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;
import cn.nicecoder.pojo.Discuss;
import cn.nicecoder.pojo.News;
import cn.nicecoder.util.AddressUtil;
import cn.nicecoder.util.BizConstant;
import cn.nicecoder.util.DBUtil;
import cn.nicecoder.util.IPUtil;
import cn.nicecoder.util.Sql;
import cn.nicecoder.util.StringUtil;

/**
 * 所有请求都进到这里
 *-------------------------------
 * @author longtian
 * @date 2018年4月2日下午10:38:35
 * @description nicecoder.cn
 *-------------------------------
 */

@WebServlet(name="coreServlet", urlPatterns="/coreServlet")  
public class CoreServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}
	
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String bizCode = req.getParameter("bizCode");
		DBUtil dbUtil = new DBUtil();
		resp.setCharacterEncoding("UTF-8"); 
	    resp.setContentType("text/html;charset=UTF-8"); 
		
		if(BizConstant.BIZ_NEWS_0.getpCode().equals(bizCode)) {//新闻插入
			//获取参数
			String id = req.getParameter("id");
			
			String title = req.getParameter("title");
			String type = req.getParameter("type");
			String content = req.getParameter("content");
			String cover = req.getParameter("cover");
			
			int author = (int) req.getSession().getAttribute("id");
			String pdate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			String click = "0";
			
			Connection conn = dbUtil.getConnection();
			try {
				// 查询数据库
				PreparedStatement pst = null;
				
				// 插入or更新数据库
				if("0".equals(id)){
					pst = conn.prepareStatement(Sql.SQL_NEWS_INSERT);
				}else{
					pst = conn.prepareStatement(Sql.SQL_NEWS_UPDATE);
					pst.setInt(8, Integer.parseInt(id));
				}
				pst.setString(1, cover);
				pst.setString(2, title);
				pst.setString(3, content);
				pst.setString(4, type);
				pst.setInt(5, author);
				pst.setString(6, pdate);
				pst.setString(7, click);
				pst.execute();
				
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//重定向
			resp.sendRedirect(req.getContextPath()+"/coreServlet?bizCode=1");
			
		}else if(BizConstant.BIZ_NEWS_1.getpCode().equals(bizCode)) {//分页查询所有新闻
			Connection conn = dbUtil.getConnection();
			ResultSet rs = null;
			try {
				String pageNo = req.getParameter("pageNo");
				String pageSize = req.getParameter("pageSize");
				
				if(StringUtil.isEmpty(pageNo)) {
					pageNo = "0";
				}
				if(StringUtil.isEmpty(pageSize)) {
					pageSize = "10";
				}
				String username = (String) req.getSession().getAttribute("username");
				String renyuan = (String) req.getSession().getAttribute("renyuan");
				PreparedStatement pst;
				if(renyuan.equals("1")){
					pst = conn.prepareStatement(Sql.SQL_NEWS_SELECTALL);
				}
				else{
					pst = conn.prepareStatement(Sql.SQL_NEWS_SELECTTD);
				}
				
				int start = Integer.parseInt(pageNo) * Integer.parseInt(pageSize);
				int end = start + Integer.parseInt(pageSize);
				if(renyuan.equals("0")){
					int id = (int) req.getSession().getAttribute("id");
					pst.setInt(1, id);
					pst.setInt(2, start);
					pst.setInt(3, end);
				}else{
					pst.setInt(1, start);
					pst.setInt(2, end);
				}
				rs = pst.executeQuery();
				
				//查询结果返回页面
				List<News> newsList = new ArrayList<News>();
				while(rs.next()) {
					News news = new News(rs.getInt("id"), rs.getString("img"), rs.getString("title"),
							new String(rs.getString("content").getBytes("ISO-8859-1"),"UTF-8"), rs.getString("clsname"), rs.getInt("author"),
							rs.getString("pudate"), rs.getString("click"));
					newsList.add(news);
				}
				
				int newsCount = newsList.size();
				int size = Integer.parseInt(pageSize);
				int pageCount = newsCount / size;
				if(newsCount % size != 0){
					pageCount ++;
				}
				
				req.setAttribute("pageCount", pageCount);
				req.setAttribute("newsList", newsList);
				req.setAttribute("pageNo", pageNo);
				
				req.setAttribute("username", username);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//返回页面
			this.getServletContext().getRequestDispatcher("/WEB-INF/admin.jsp").forward(req, resp);
			
		}else if(BizConstant.BIZ_NEWS_2.getpCode().equals(bizCode)) {//查询单条新闻
			String id = req.getParameter("id");
			Connection conn = dbUtil.getConnection();
			ResultSet rs = null;
			News news = null;
			try {
				PreparedStatement pst = conn.prepareStatement(Sql.SQL_NEWS_SELECTBYID);
				pst.setInt(1, Integer.parseInt(id));
				rs = pst.executeQuery();
				while(rs.next()){
					news = new News(rs.getInt("id"), rs.getString("img"), rs.getString("title"),
							new String(rs.getString("content").getBytes("ISO-8859-1"),"UTF-8"), rs.getString("type"), rs.getInt("author"),
							rs.getString("pudate"), rs.getString("click"));
					req.setAttribute("news", news);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//json返回
			PrintWriter pw = resp.getWriter();
			JSONObject jb = new JSONObject();
			pw.write(jb.fromObject(news).toString());
			pw.flush();
			pw.close();
			
		}else if(BizConstant.BIZ_NEWS_3.getpCode().equals(bizCode)) {//删除单条新闻
			String id = req.getParameter("id");
			Connection conn = dbUtil.getConnection();
			try {
				PreparedStatement pst = conn.prepareStatement(Sql.SQL_NEWS_DELETE);
				pst.setInt(1, Integer.parseInt(id));
				pst.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//重定向
			resp.sendRedirect(req.getContextPath()+"/coreServlet?bizCode=1");
			
		}else if(BizConstant.BIZ_NEWS_4.getpCode().equals(bizCode)){//新闻详情
			String id = req.getParameter("id");
			Connection conn = dbUtil.getConnection();
			ResultSet rs = null;
			News news = null;
			try {
				PreparedStatement pst = conn.prepareStatement(Sql.SQL_NEWS_SELECTBYID);
				pst.setInt(1, Integer.parseInt(id));
				rs = pst.executeQuery();
				while(rs.next()){
					news = new News(rs.getInt("id"), rs.getString("img"), rs.getString("title"),
							new String(rs.getString("content").getBytes("ISO-8859-1"),"UTF-8"), rs.getString("clsname"), rs.getInt("author"),
							rs.getString("pudate"), rs.getString("click"));
					req.setAttribute("news", news);
				}
				
				//查询该条新闻下的评论
				pst = conn.prepareStatement(Sql.SQL_DISSCUSS_SELECTBYID);
				pst.setString(1, "0");
				pst.setString(2, id);
				rs = pst.executeQuery();
				List<Discuss> disList = new ArrayList<Discuss>();
				while(rs.next()){
					System.out.println(new String(rs.getString("content").getBytes("ISO-8859-1"),"UTF-8"));
					Discuss discuss = new Discuss(rs.getInt("id"), rs.getString("type"), rs.getString("discussId"),
							new String(rs.getString("content").getBytes("ISO-8859-1"),"UTF-8"), rs.getString("userId"), rs.getString("pudate"),
							rs.getString("agree"));
					//查询评论下的评论
					pst = conn.prepareStatement(Sql.SQL_DISSCUSS_SELECTBYID);
					pst.setString(1, "1");
					pst.setString(2, discuss.getId()+"");
					ResultSet rssub = pst.executeQuery();
					List<Discuss> disListsub = new ArrayList<Discuss>();
					while(rssub.next()){
						Discuss discusssub = new Discuss(rssub.getInt("id"), rssub.getString("type"), rssub.getString("discussId"),
								new String(rssub.getString("content").getBytes("ISO-8859-1"),"UTF-8"), rssub.getString("userId"), rssub.getString("pudate"),
								rssub.getString("agree"));
						disListsub.add(discusssub);
					}
					discuss.setList(disListsub);
					disList.add(discuss);
				}
				news.setList(disList);
				
				//查出上一条和下一条的id
				pst = conn.prepareStatement(Sql.SQL_DISSCUSS_NEARID);
				pst.setString(1, id);
				pst.setString(2, id);
				ResultSet rsnear = pst.executeQuery();
				while(rsnear.next()){
					if(rsnear.getInt("dir") == 0){
						news.setPre(rsnear.getInt("id"));
						news.setPreTitle(StringUtil.lengthControl(rsnear.getString("title"), 15));
					}else if(rsnear.getInt("dir") == 1){
						news.setNext(rsnear.getInt("id"));
						news.setNextTitle(StringUtil.lengthControl(rsnear.getString("title"), 15));
					}
				} 
				
				news.setClick(Integer.parseInt(news.getClick()) + 1 + "");
				req.setAttribute("news", news);
				
				//点击量+1
				pst = conn.prepareStatement(Sql.SQL_NEWS_UPDATE_CILICK);
				pst.setString(1, news.getClick());
				pst.setInt(2, Integer.parseInt(id));
				pst.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			//返回页面
			this.getServletContext().getRequestDispatcher("/news.jsp").forward(req, resp);
			
		}else if(BizConstant.BIZ_NEWS_5.getpCode().equals(bizCode)){//提交评论
			String type = req.getParameter("type");
			String id = req.getParameter("id");
			String content = new String(req.getParameter("content"));
			String pdate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());

			Connection conn = dbUtil.getConnection();
			try {
				PreparedStatement pst = conn.prepareStatement(Sql.SQL_DISSCUSS_INSERT);
				pst.setString(1, type);
				pst.setString(2, id);
				pst.setString(3, content);
				if(req.getSession().getAttribute("username") == null){
					pst.setString(4, "网友From " + AddressUtil.getAddresses("ip=" + IPUtil.getIpAddress(req), "utf-8"));
				}else{
					pst.setString(4, (String)req.getSession().getAttribute("username"));
				}
				pst.setString(5, pdate);
				pst.setString(6, "0");
				
				pst.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			PrintWriter pw = resp.getWriter();
			JSONObject jb = new JSONObject();
			jb.put("result", "ok");//返回的不是json会进入error
			pw.write(jb.toString());
			pw.flush();
			pw.close();
			
		}else if(BizConstant.BIZ_NEWS_6.getpCode().equals(bizCode)){//点赞
			String type = req.getParameter("type");
			String id = req.getParameter("id");
			String count = req.getParameter("count");
			String pdate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			String userId = IPUtil.getIpAddress(req);
			
			Connection conn = dbUtil.getConnection();
			PreparedStatement pst = null;
			PreparedStatement pstagree = null;
			String agree = null;
			try {
				//更新discuss表或者是news表
				if("1".equals(type)){
					pstagree = conn.prepareStatement(Sql.SQL_DISSCUSS_UPDATEAGREE);
				}else{
					//新闻点赞
				}
				
				//查询是否已经点赞
				pst = conn.prepareStatement(Sql.SQL_AGREE_SELECTBYUSERID);
				pst.setString(1, userId);
				pst.setString(2, id);
				ResultSet rs = pst.executeQuery();
				if(rs.next()){
					pst = conn.prepareStatement(Sql.SQL_AGREE_DELETE);
					pst.setString(1, userId);
					pst.setString(2, id);
					pst.execute();
					count = Integer.parseInt(count) - 1 + ""; 
					agree = "0";
				}else{
					pst = conn.prepareStatement(Sql.SQL_AGREE_INSERT);
					pst.setString(1, type);
					pst.setString(2, id);
					pst.setString(3, userId);
					pst.setString(4, pdate);
					pst.execute();
					count = Integer.parseInt(count) + 1 + "";
					agree = "1";
				}
				
				pstagree.setString(1, count);
				pstagree.setString(2, id);
				pstagree.execute();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			//json返回
			PrintWriter pw = resp.getWriter();
			JSONObject jb = new JSONObject();
			jb.put("agree", agree);
			jb.put("count", count);
			pw.write(jb.toString());
			pw.flush();
			pw.close();
		}else if(BizConstant.BIZ_NEWS_7.getpCode().equals(bizCode)) {//按条件分页查询新闻
			Connection conn = dbUtil.getConnection();
			ResultSet rs = null;
			JSONObject jb = new JSONObject();
			try {
				String pageNo = req.getParameter("pageNo");
				String pageSize = req.getParameter("pageSize");
				String type = req.getParameter("type");
				String keyWord = req.getParameter("keyWord");
				//System.out.println(type);
				if(StringUtil.isEmpty(pageNo)) {
					pageNo = "0";
				}
				if(StringUtil.isEmpty(pageSize)) {
					pageSize = "5";
				}
				int start = Integer.parseInt(pageNo) * Integer.parseInt(pageSize);
				int end = start + Integer.parseInt(pageSize);
				String orgSql = Sql.SQL_NEWS_SELECTALLINDEX;
				PreparedStatement pst = null;
				if(StringUtil.isEmpty(type) && StringUtil.isEmpty(keyWord)){
					pst = conn.prepareStatement(orgSql + " LIMIT ?, ?");
					pst.setInt(1, start);
					pst.setInt(2, end);
				}else if(StringUtil.isNotEmpty(type) && StringUtil.isEmpty(keyWord)){
					pst = conn.prepareStatement(orgSql + "WHERE t.clsid = ?" + " LIMIT ?, ?");
					pst.setInt(1, Integer.parseInt(type));
					pst.setInt(2, start);
					pst.setInt(3, end);
				}else if(StringUtil.isEmpty(type) && StringUtil.isNotEmpty(keyWord)){
					pst = conn.prepareStatement(orgSql + "WHERE content like ?" + " LIMIT ?, ?");
					pst.setString(1, "%" + keyWord + "%");
					pst.setInt(2, start);
					pst.setInt(3, end);
				}else if(StringUtil.isNotEmpty(type) && StringUtil.isNotEmpty(keyWord)){
					pst = conn.prepareStatement(orgSql + "WHERE t.clsid = ? AND content like ?" + " LIMIT ?, ?");
					pst.setInt(1, Integer.parseInt(type));
					pst.setString(2, "%" + keyWord + "%");
					pst.setInt(3, start);
					pst.setInt(4, end);
				}
				


				rs = pst.executeQuery();
				
				//查询结果返回页面
				List<News> newsList = new ArrayList<News>();
				while(rs.next()) {
					String content = new String(rs.getString("content").getBytes("ISO-8859-1"));
					content = StringUtil.lengthControl(StringUtil.delHtmlTag(content), 150);
					News news = new News(rs.getInt("id"), rs.getString("img"), rs.getString("title"),
							content, rs.getString("clsname"), rs.getInt("author"),
							rs.getString("pudate"), rs.getString("click"));
					newsList.add(news);
				}
				
				int newsCount = newsList.size();
				int size = Integer.parseInt(pageSize);
				int pageCount = newsCount / size;
				if(newsCount % size != 0 && newsCount > size){
					pageCount ++;
				}
				if("0".equals(pageNo)){
					req.setAttribute("pageCount", pageCount);
					req.setAttribute("newsList", newsList);
					req.setAttribute("pageNo", pageNo);
					req.setAttribute("keyWord", keyWord);
					//返回页面
					this.getServletContext().getRequestDispatcher("/index.jsp").forward(req, resp);
				}else{
					jb.put("pageCount", pageCount);
					jb.put("newsList", newsList);
					jb.put("pageNo", pageNo);
					PrintWriter pw = resp.getWriter();
					pw.write(jb.toString());
					pw.flush();
					pw.close();
				}
				
			} catch (SQLException e) {
				e.printStackTrace();
			} finally{
				try {
					rs.close();
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}else if(bizCode.equals("8")){
			String username = req.getParameter("username");
			String mima = req.getParameter("password");
			Connection conn = dbUtil.getConnection();
			ResultSet rs = null;
			
			byte b[] = username.getBytes();
			username = new String(b,"UTF-8");
			
			if(username.length()>5&&mima.length()>5){
				try{
				PreparedStatement pst = null;
				pst = conn.prepareStatement(Sql.SQL_USER_INSERT);
				pst.setString(1, username);
				pst.setString(2, mima);
				pst.setInt(3, 0);
				pst.execute();
				
				System.out.println("1");
				this.getServletContext().getRequestDispatcher("/success.jsp").forward(req, resp);
				
				}catch(SQLException e){
					this.getServletContext().getRequestDispatcher("/lose.jsp").forward(req, resp);
				}
			}else{
			System.out.println("2");
			this.getServletContext().getRequestDispatcher("/lose.jsp").forward(req, resp);
			}
		}else if(bizCode.equals("9")){//退出
			req.getSession().removeAttribute("username");
			this.getServletContext().getRequestDispatcher("/login.jsp").forward(req, resp);
		}
		
		
	}
	
}
