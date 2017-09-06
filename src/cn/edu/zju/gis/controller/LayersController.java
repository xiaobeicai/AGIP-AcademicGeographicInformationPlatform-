package cn.edu.zju.gis.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.ibatis.annotations.Case;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;

import cn.edu.zju.gis.po.Layers;
import cn.edu.zju.gis.po.Place;
import cn.edu.zju.gis.service.LayersService;
import cn.edu.zju.gis.util.Analyse;


@Controller
public class LayersController {
	@Autowired
	private LayersService layersService;
	
	@RequestMapping(value = "/addLayers", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	//如果将来想返回json，则produce="text/json'charset=UTF-8"
	@ResponseBody
	public String addLayers(
			@RequestParam(value="layername") String layername,  	
			@RequestParam(value="file") MultipartFile file,
			@RequestParam(value="appendDataSrc") String appendDataSrc,
			HttpSession session) throws Exception {
		
		int type = (Integer)session.getAttribute("LayerType");
		String filename = file.getOriginalFilename();
		//获取文件的大小
		int length = (int) file.getSize();
		//获取文件的输入流
		InputStream inputStream= file.getInputStream();
		//构造合适的字节数组长度用于读取输入流中的字节
		byte b[]=new byte[(int) file.getSize()];
		//读取输入流中的字节到字节数组b中
		inputStream.read(b);
		//关闭输入流，释放资源 
		inputStream.close();
		//创建文件夹
		//TODO 服务器的上传的文件的存放路径的设定 暂时还没处理
		File file2 = new File("e:\\layers");
		if(!file2.exists()) {
			file2.mkdirs();
		}
		//构造文件的存储路径
		String storeLocation = "e:\\layers\\" + filename;
		//构造输出流，流向该文件
		FileOutputStream fileOutputStream = new FileOutputStream(storeLocation);
		//将字节数组中的数据写入到文件中
		fileOutputStream.write(b);
		//关闭输出流，释放资源
		fileOutputStream.close();
		//解析编码格式
		File filet = new File(storeLocation);
        InputStream in= new java.io.FileInputStream(filet);
        byte[] test = new byte[3];
        in.read(test);
        in.close();
        String encode="gbk";
        if (test[0] == -17 && b[1] == -69 && b[2] == -65) encode="utf-8";

		//解析存储在服务器端的csv数据
		
        InputStreamReader isr = new InputStreamReader(new FileInputStream(filet),encode);
		BufferedReader bufferedReader = new BufferedReader(isr);
		
		String line = null;
		String content = null;
		line = bufferedReader.readLine();//读取第一行文件
		
		String title[] = line.split(",");//读出文件头
		
		int count = title.length;//这个是列数
		int hasXY = 0;//是否有XY字段
		boolean  seniorCondition = false;//字段是否齐全的检查变量 = = 
		switch(type)//按照图层类型解析文件 = = 其实我不觉得这样的结构好…… 应该构造好一个class之类的东西 by asayuki
		{
			case 0://分层设色图数据 标准 列名 "地名" "数值" "X" "Y" "附加信息"
				//感觉可以直接构造个HashMap啊 = = 
				if(!title[0].equals("地名") && count<2) {
					bufferedReader.close();
					//TODO 删除服务器上的临时文件
					File file3 = new File(storeLocation);
					file3.delete();
					//返回的东西可以再慎重点 最好改成数字编码
					return "您上传的数据不符合规范";
				}
				else {
					title[0]="name";
					for(int i = 1; i< count; i++) {
						if(title[i].equals("数值")) {
							title[1]="value";
							seniorCondition = true;
						}
						if("x".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
						if("y".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
					}
					//如果满足条件则开始对后续文件进行解析
					if(seniorCondition) {
						if(hasXY == 2)
							content = Analyse.AnalyseCSV(bufferedReader, 0 , true,title);
						else
							content = Analyse.AnalyseCSV(bufferedReader, 0 , false,title);
						break;
					}
					else {
						bufferedReader.close();
						//TODO 删除服务器上的CSV文件
						File file3 = new File(storeLocation);
						file3.delete();				
						return "您上传的数据不符合规范";
					}
				}
			case 1://�ȼ�����ͼ
				//�����ֶ�Ҫ��ӵ�е����ֶβ����ֶ�������Ϊ2
				if(!title[0].equals("地名") && count<2) {
					bufferedReader.close();
					//ɾ���洢��csv����
					File file3 = new File(storeLocation);
					file3.delete();				
					return "您上传的数据不符合规范";
				}
				else {
					title[0]="name";
					for(int i = 1; i< count; i++) {
						if(title[i].equals("数值")) {
							title[1]="value";
							seniorCondition = true;
						}
						if("x".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
						if("y".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
					}
					//�߼��ֶ�Ҫ��ӵ�С���ֵ���ֶΣ������ж��Ƿ�ӵ�о�γ��
					if(seniorCondition) {
						if(hasXY == 2)
							content = Analyse.AnalyseCSV(bufferedReader, 1 , true, title);
						else
							content = Analyse.AnalyseCSV(bufferedReader, 1 , false,title);
						break;
					}
					else {
						bufferedReader.close();
						//ɾ���洢��csv����
						File file3 = new File(storeLocation);
						file3.delete();				
						return "您上传的数据不符合规范";
					}
				}
			case 2://��ͼ
				//�����ֶ�Ҫ��ӵ�е����ֶβ����ֶ�������Ϊ1
				if(!title[0].equals("地名") && count<1) {
					bufferedReader.close();
					//ɾ���洢��csv����
					File file3 = new File(storeLocation);
					file3.delete();				
					return "您上传的数据不符合规范";
				}
				else {
					title[0]="name";
					for(int i = 1; i< count; i++) {
						title[1]="value";
						if("x".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
						if("y".equalsIgnoreCase(title[i])) {
							hasXY++;
						}
					}	
					if(hasXY == 2)
						content = Analyse.AnalyseCSV(bufferedReader, 2 , true, title);
					else
						content = Analyse.AnalyseCSV(bufferedReader, 2 , false, title);
					break;					
				}
			case 3://线图层
				//这里牵扯到上传的数据的线的表现形式的问题 现在暂时还是标准的OGC的geometry的字符串表出
				if(!title[0].equalsIgnoreCase("id") && !title[1].equals("the_geom") && count<2) {
					bufferedReader.close();
					//ɾ���洢��csv����
					File file3 = new File(storeLocation);
					file3.delete();				
					return "您上传的数据不符合规范";
				}
				else {
					//有必要再做调整 或许会改成新版本的代码吧 = = 
					content = Analyse.AnalyseCSV3(bufferedReader, title);
					break;					
				}
			default:
				break;
		}
				
		Layers layer = new Layers();
		
		layer.setAccessibility(true);
		 
		
		layer.setLayername(layername);
		layer.setType(type);
		layer.setStorelocation(storeLocation);
		layer.setUserid((Integer)session.getAttribute("userid"));
		layer.setDatacontent(content);
		layer.setAppendDataSrc(appendDataSrc);
		boolean bool = layersService.addLayers(layer);
		if(bool)
			return "success";
		else
			return "fail";
	}
	
	//emmm 等着被重写吧
	@RequestMapping(value = "/searchLayers",method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String searchLayers(String keyword,String type,HttpSession session) throws Exception {
		List<Layers> list = layersService.searchLayers(keyword,Integer.parseInt(type));
		Integer userid = (Integer)session.getAttribute("userid");
		if(userid==null) userid=0;
		List<Layers> result = new ArrayList<Layers>();
		for(Layers layer:list)
		{
			if(layer.getAccessibility() || layer.getUserid()==userid)
				result.add(layer);
		}
		Gson gson = new Gson();
		return gson.toJson(result);
	}
	
	@RequestMapping(value = "/setLayerType",method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String setLayerType(int LayerType,HttpSession session) throws Exception {
		session.setAttribute("LayerType", LayerType);	
		return "success";
	}
}
