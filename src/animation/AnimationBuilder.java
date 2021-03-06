package animation;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.vecmath.Vector3d;

import org.dom4j.DocumentException;
import org.math.plot.Plot2DPanel;

import com.jme3.animation.Animation;
import com.jme3.animation.AnimationFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import dataEntity.AirportInfo;
import dataEntity.Road;
import dataEntity.RoadEdge;
import dataEntity.RoadPoint;
import dataEntity.RoadWay;
import dataEntity.TrackPoint;
import mapMatch.MapMatch;
import mapMatch.road.RoadXMLUtility;
import pathSmooth.Mercator;

public class AnimationBuilder {

        private AnimationFactory animationFactory;
        public ArrayList<TrackPoint> trackPoints;
        private float animTime;
        
        //旋转关键帧相关变量
        private int rotationKeyFrames;
		protected float[] rotKeyFramesTimes;
        protected float[] keyFramesHeading;
        protected float[] keyFramesPitch;
        protected float[] keyFramesBank;
        //平移关键帧相关变量
        private int transKeyFrames;
        protected float[] transKeyFramesTimes;
        protected Vector3f[] keyFramesTranslation;
       
        //路网数据
        Road road=null;
        
        //机场中心经纬度，及其平面坐标
    	double lon;
    	double lat;
    	double[] temp;
    	
    	public void setLonLat(double lon,double lat) {
    		this.lon=lon;
    		this.lat=lat;
    		temp=Mercator.lonLat2Mercator(lon, lat);
    	}
    	
    	
        
        public AnimationBuilder() {

        }
        
        public AnimationBuilder(ArrayList<TrackPoint> trackPoints) {
        	this.trackPoints=trackPoints;
        }
        
        public void setTrackPoints(ArrayList<TrackPoint> trackPoints) {
        	this.trackPoints=trackPoints;
        }
        
        public Animation buildAnimation() {
        	
    		// 读取路网数据
    	   road = null;
    		try {
    			road = RoadXMLUtility.ReadXML();
    		} catch (DocumentException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}

        	computeTime();
        	computePosition();
        	computeRotation();
        	animationFactory = new AnimationFactory(animTime, "anim", 30);
        	
        	for(int i=0;i<rotationKeyFrames;i++) {
	        //Creating a rotation keyFrame
	        animationFactory.addTimeRotationAngles(rotKeyFramesTimes[i], keyFramesPitch[i],keyFramesHeading[i], keyFramesBank[i]);
        	}
        	
        	
        	for(int i=0;i<transKeyFrames;i++) {
	        //creating a translation keyFrame         
	        animationFactory.addTimeTranslation(transKeyFramesTimes[i], keyFramesTranslation[i]);
	        
        	}
        	
        	System.out.println("原始轨迹点数："+trackPoints.size());
        	System.out.println("位移关键帧数："+transKeyFrames);
        	System.out.println("旋转关键帧数："+rotationKeyFrames);
        	Animation animation= animationFactory.buildAnimation();
        	//清空数组
        	animationFactory=null;
//        	trackPoints.clear();
        	animTime=0;
        	rotationKeyFrames=0;
        	transKeyFrames=0;
        	rotKeyFramesTimes=null;
        	transKeyFramesTimes=null;
        	keyFramesTranslation=null;
        	keyFramesHeading=null;
            keyFramesPitch=null;
            keyFramesBank=null;
        	
        	return animation;
        }
        
        
        
        /**
         * 计算动画总时间
         */
        protected void computeTime() {
        	    //计算动画总时间
        		Long t1=trackPoints.get(0).getDate().getTime();
    			Long t2=trackPoints.get(trackPoints.size()-1).getDate().getTime();
    			animTime= (t2-t1)/1000.0f;//秒
         }
        
        
        /**
         * 使用算法处理轨迹，并提取位移关键帧
         */
        protected void computePosition()  {
        	double xy[];
        	double longitude;
        	double latitude;
        	double altitude; 
        	//----------------------计算translation关键帧数组-------------------------------//
        	ArrayList<TrackPoint> copy=new ArrayList<TrackPoint>();
			try {
				copy = deepCopy(trackPoints);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
			    e.printStackTrace();
			}
			//打印原始轨迹点
			System.out.println("原始");
			System.out.println(copy);
			
			//去除位置重复点 
//			copy=pathSmooth.RemoveDuplicatesUtil.RemoveDuplicates(copy);
//			System.out.println("去重");
//			System.out.println(copy);
			
			//地图匹配算法
			copy=MapMatch.SimpleMapMatch(road,copy);
	        System.out.println("地图匹配");
			System.out.println(copy);
			
			
//			ArrayList<RoadWay> roadWays = road.getRoadWays();
//			// 从RoadWay中抽取所有的RoadEdge
//			ArrayList<RoadEdge> roadEdges = new ArrayList<RoadEdge>();
//			int wayNumber = roadWays.size();
//			for (int i = 0; i < wayNumber; i++) {
//				roadEdges.addAll(roadWays.get(i).getRoadEdges());
//			}
//			// 定义颜色
//			Color red = new Color(255, 0, 0);
//			Color black = new Color(0, 0, 0);
//			Color blue = new Color(0, 0, 255);
//			Color green = new Color(0, 255, 0);
//
//			// 定义画图面板
//			Plot2DPanel plot = new Plot2DPanel();
//
//			// 遍历路径边，绘制路径边（黑色线条）
//			int edgeNumber = roadEdges.size();
//			RoadEdge tempEdge = null;
//			RoadPoint roadPoint1 = null;
//			RoadPoint roadPoint2 = null;
//			for (int j = 0; j < edgeNumber; j++) {
//				tempEdge = roadEdges.get(j);
//				roadPoint1 = tempEdge.getP1();
//				roadPoint2 = tempEdge.getP2();
//				double[] point1 = { roadPoint1.getLongitude(), roadPoint1.getLatitude() };
//				double[] point2 = { roadPoint2.getLongitude(), roadPoint2.getLatitude() };
//				plot.addLinePlot("", black, point1, point2);
//			}
		
			//角度判断算法，简化轨轨迹
			copy=pathSmooth.AngleSimplifyUtil.DeleteErrorPoint(copy,90);
			System.out.println("折返点处理");
			System.out.println(copy);
//			
//			
//			for (int i = 0; i < copy.size() - 1; i++) {
//			TrackPoint Point1 = copy.get(i);
//			TrackPoint Point2 = copy.get(i + 1);
//			double[] point1 = { Point1.getLongitude(), Point1.getLatitude() };
//			double[] point2 = { Point2.getLongitude(), Point2.getLatitude() };
//
//			plot.addLinePlot("", red, point1, point2);
//		}
			
//			// set the label of the plot panel
//			plot.setAxisLabel(0, " longitude");
//			plot.setAxisLabel(1, "latuitude");
//			plot.setSize(1200, 1200);
//			// create a frame
//			JFrame frame = new JFrame("A plot test");
//			// set the size of the frame
//			frame.setSize(1200, 1200);
//			// set the content of the frame as the plot panel
//			frame.setContentPane(plot);
//			frame.setVisible(true);
			
			
			
			
			
			//道格拉斯佩格算法，简化轨迹，阈值单位m
//			copy=pathSmooth.DouglasPeuckerUtil.DouglasPeucker(copy,20);
//			System.out.println("轨迹简化处理");
//			System.out.println(copy);
        	
        	transKeyFrames=copy.size();
        	//定义简化后平移帧数组和对应的时间数组并赋值
        	transKeyFramesTimes=new float[transKeyFrames];
        	keyFramesTranslation=new Vector3f[transKeyFrames];
        	//第一个关键点时间
        	Long t1=trackPoints.get(0).getDate().getTime();
        	
        	for(int k=0;k<transKeyFrames;k++) {
  			 latitude=copy.get(k).getLatitude();
  			 longitude= copy.get(k).getLongitude();
  			 xy=Mercator.lonLat2Mercator(longitude, latitude);//坐标转换
  			 
  			 altitude= copy.get(k).getAltitude()*0.3048f;//英尺转米
  			 
//  			 System.out.println(xy[0]-temp[0]+","+(-xy[1]+temp[1]));
  			 
  			 keyFramesTranslation[k]=new Vector3f(  (float)(xy[0]-temp[0]),(float)altitude,(float)(-xy[1]+temp[1]));
  			 
  			 transKeyFramesTimes[k]=(copy.get(k).getDate().getTime()-t1)/1000.0f;
  			}
        	copy.clear();

         }

        
        /**
         * 提取旋转关键帧
         * heading，估算pitch
         * 不考虑bank计算
         */
        protected void computeRotation() {
        	
        	//第一个关键点时间
        	Long t1=trackPoints.get(0).getDate().getTime();
        	
         	//----------------------计算rotation关键帧数组-------------------------------//
        	ArrayList<TrackPoint> copy=new ArrayList<TrackPoint>();
			try {
				copy = deepCopy(trackPoints);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			copy=pathSmooth.RotationSimplifyUtil.RotationSimplify(copy,1);
			rotationKeyFrames=copy.size();
        	//定义旋转帧数组并赋值
            keyFramesHeading=new float[rotationKeyFrames];
            keyFramesPitch=new float[rotationKeyFrames];
            keyFramesBank=new float[rotationKeyFrames];
            rotKeyFramesTimes=new float[rotationKeyFrames];
            
//            System.out.println(rotationKeyFrames);
            
        	for(int k=0;k<rotationKeyFrames-1;k++) {
        		
        	 keyFramesHeading[k]=  (float) (- FastMath.DEG_TO_RAD*(copy.get(k).getHeading()));//注意负号
        	 
//        	 System.out.println(keyFramesHeading[k]*FastMath.RAD_TO_DEG);
        	 
        	 keyFramesPitch[k]=(float) computePitch(copy.get(k), copy.get(k+1));
        	 
//        	 System.out.println("俯仰角： "+keyFramesPitch[k]);
        	 
  			 keyFramesBank[k]=0.0f;
  			 rotKeyFramesTimes[k]=(copy.get(k).getDate().getTime()-t1)/1000.0f;
  			}

        	//最后一帧处理
        	keyFramesHeading[rotationKeyFrames-1]= (float) (- FastMath.DEG_TO_RAD*(copy.get(rotationKeyFrames-1).getHeading()));//注意负号
        	
       	    keyFramesPitch[rotationKeyFrames-1]=keyFramesPitch[rotationKeyFrames-2];
       	    
 			keyFramesBank[rotationKeyFrames-1]=0.0f;
 			
 			rotKeyFramesTimes[rotationKeyFrames-1]=(copy.get(rotationKeyFrames-1).getDate().getTime()-t1)/1000.0f;
 			
// 			System.out.println("--------------------------------");
 			
 			for(int k=0;k<rotationKeyFrames;k++) {
 				
// 				System.out.println(rotKeyFramesTimes[k]+","+FastMath.RAD_TO_DEG*keyFramesHeading[k]);
 				
 			}
 			
 			copy.clear();
         }
        

        /**
         * 根据两点估计俯仰角pitch
         */
        protected double computePitch(Vector3d v1,Vector3d v2) {
			return Math.atan(   (v2.y-v1.y)/Math.sqrt(   Math.pow(v2.x-v1.x, 2)+Math.pow(v2.z-v1.z, 2)  )   );
        }
        protected double computePitch(TrackPoint p1,TrackPoint p2) {
        	double longitude;
        	double latitude;
        	double altitude; 
        	double xy[];
        	Vector3d v1;
			Vector3d v2;
        	
        	latitude=p1.getLatitude();
 			longitude= p1.getLongitude();
 			xy=Mercator.lonLat2Mercator(longitude, latitude);//坐标转换 
 			altitude= p1.getAltitude()*0.3048f;//英尺转米
 			v1=new Vector3d(xy[0]-temp[0],altitude,-xy[1]+temp[1]);
 			
 			latitude=p2.getLatitude();
 			longitude= p2.getLongitude();
 			xy=Mercator.lonLat2Mercator(longitude, latitude);//坐标转换 
 			altitude= p2.getAltitude()*0.3048f;//英尺转米
 			v2=new Vector3d(xy[0]-temp[0],altitude,-xy[1]+temp[1]);
 			
 			
			return computePitch(v1,v2);
        }
        
 
        /**
         * 使用序列化手段，完成轨迹点的复制
         */
        public static <T> ArrayList<T> deepCopy(ArrayList<T> src) throws IOException, ClassNotFoundException {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(src);

            ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
            ObjectInputStream in = new ObjectInputStream(byteIn);

            ArrayList<T> copy_list = (ArrayList<T>) in.readObject();
            return copy_list;
        }


}

