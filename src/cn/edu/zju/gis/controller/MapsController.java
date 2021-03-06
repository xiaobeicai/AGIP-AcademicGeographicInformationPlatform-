package cn.edu.zju.gis.controller;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import cn.edu.zju.gis.po.MapLayer;
import cn.edu.zju.gis.po.Maps;
import cn.edu.zju.gis.po.MapsCustom;
import cn.edu.zju.gis.po.MapsVo;
import cn.edu.zju.gis.po.Users;
import cn.edu.zju.gis.service.MapsService;
import cn.edu.zju.gis.service.UsersService;

@Controller
public class MapsController 
{
	@Autowired
	private MapsService mapsService;
	
	@Autowired
	private UsersService usersService;
	
	@RequestMapping("/addLayerToMap")
	public ModelAndView addLayerToMap(int mapid,int layerid) throws Exception
	{
		System.out.println(mapid+":2333:"+layerid);
		ModelAndView modelAndView = new ModelAndView();
		return modelAndView;
	}
	
	@RequestMapping(value = "/getMapList", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String getMapList(int userid) throws Exception
	{
		List<Maps> maps = mapsService.getMapList();
		List<Maps> result = new ArrayList<Maps>();
		for(Maps map:maps)
		{
			if(map.getAccessibility()==1 || userid==map.getUserid())
				result.add(map);
		}
		Gson gson = new Gson();
		return gson.toJson(result);
	}
	
	@RequestMapping(value = "/getMapListForSearch", method = RequestMethod.GET,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String getMapListForSearch(MapsVo querymap,HttpSession session) throws Exception
	{
		Integer userid = (Integer)session.getAttribute("userid");
		if(null==userid) userid=0;//游客权限注入
		querymap.setUserid(userid);
		querymap.setAccessibility(1);
		querymap.setAddable(1);
		List<Maps> maps = mapsService.getMapList2(querymap);
		Gson gson = new Gson();
		String rows = gson.toJson(maps);
		int count = mapsService.countMaps(querymap);
		return "{\"total\":"+count+",\"rows\":"+rows+"}";	
	}
	
	@RequestMapping(value = "/getMapListForIndex", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String getMapListForIndex(String type) throws Exception
	{
		MapsVo querymap = new MapsVo();
		querymap.setLimit(6);
		//here the type can be other String ,you just set the right map like this
		if(type.equals("1")){querymap.setMaptype(1);}
		else if(type.equals("2")){querymap.setMaptype(2);}
		else if(type.equals("3")){querymap.setMaptype(3);}
		List<Maps> maps = mapsService.getMapListForIndex(querymap);
		
		Gson gson = new Gson();
		return gson.toJson(maps);
	}
	
	
	@RequestMapping(value = "/getMapLayerList", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String getMapLayerList(int mapid) throws Exception
	{
		List<MapLayer> layerlist = mapsService.findMapLayerByMapId(mapid);
		Gson gson = new Gson();
		return gson.toJson(layerlist);
	}
	
	@RequestMapping(value = "/getMapInfo", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String getMapInfo(int mapid) throws Exception
	{
		Maps map = mapsService.findMapById(mapid);
		Gson gson = new Gson();
		return gson.toJson(map);
	}
	
	@RequestMapping(value = "/savemap", method = RequestMethod.POST,   
	        produces = "text/html;charset=UTF-8")
	@ResponseBody
	public String savemap(String map,String maplayer,HttpSession session) throws Exception
	{
		Integer userid = (Integer)session.getAttribute("userid");
		if(null==userid) return "nouser";
		else if (usersService.checkUserAuthority(userid)==false) return "bebanned";
		Gson gson = new Gson();
		Maps mapObj = gson.fromJson(map, Maps.class);
		
        ArrayList<JsonElement> jsonObjects = gson.fromJson(maplayer, new TypeToken<ArrayList<JsonElement>>()
        {}.getType());
 
        ArrayList<MapLayer> maplayerArr = new ArrayList<>();
        for (JsonElement jsonObject : jsonObjects)
        {
        	maplayerArr.add(gson.fromJson(jsonObject, MapLayer.class));
        }
        //TODO 底图模块仍然待定 感觉可以做成底图层切换
        Maps mapForSave = new Maps(mapObj.getId(),
        		mapObj.getMapname(),
        		mapObj.getUserid(),
        		mapObj.getAccessibility(),
        		0,
        		mapObj.getMapstyle(),
        		1,
        		mapObj.getLayertree(),
        		mapObj.getMaptype()
        		);
        List<MapLayer> oldLayerlist = new ArrayList<MapLayer>();
        if(mapForSave.getId()==0)
        {
        	int insertMap = mapsService.insertMap(mapForSave);
        	
        	int mapid = mapForSave.getId();
            for (MapLayer layer : maplayerArr)
            {
            	layer.setMapid(mapid);
                int insertmaplayer = mapsService.insertMapLayer(layer);
                //DONE 利用此时layer里面的信息 去更新传入的layertree信息
                //DONE 利用前端完成了layertree的更新
            }
            return "success";
        }
        else
        {
        	int updateMap = mapsService.updateMap(mapForSave);
        	oldLayerlist = mapsService.findMapLayerByMapId(mapForSave.getId());
        	
        	for (MapLayer i : oldLayerlist)
            {
        		boolean flag=true;
        	for(MapLayer layer : maplayerArr)
        	{
        		if( layer.getMlid() == i.getMlid())
        		{
        		flag=false;
        		break;
        		}
        	}    
        	if(flag){
        		int deletemaplayer = mapsService.deleteMapLayer(i);
        	}
            }
        }
        int mapid= mapForSave.getId();
        for (MapLayer layer : maplayerArr)
        {
        	layer.setMapid(mapid);
        	if(layer.getMlid()==0)
            {
            	int insertmaplayer = mapsService.insertMapLayer(layer);
            }
            else
            {
            	boolean flag=true;
            	int updatemaplayer = mapsService.updateMapLayer(layer);
            	
            }
        }
		return "success";
	}
}
