package com.cheapRide.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.cheapRide.comparators.LyftETAComparator;
import com.cheapRide.comparators.LyftMinPriceComparator;
import com.cheapRide.comparators.UberETAComparator;
import com.cheapRide.comparators.UberMinPriceComparator;
import com.cheapRide.model.EstimateResponseModel;
import com.cheapRide.model.ResponseModel;
import com.cheapRide.model.lyft.ListLyftETAModel;
import com.cheapRide.model.lyft.ListLyftPriceModel;
import com.cheapRide.model.lyft.LyftETAModel;
import com.cheapRide.model.lyft.LyftPriceModel;
import com.cheapRide.model.uber.ListUberETAModel;
import com.cheapRide.model.uber.ListUberPriceModel;
import com.cheapRide.model.uber.UberETAModel;
import com.cheapRide.model.uber.UberPriceModel;
import com.cheapRide.service.RideEstimateService;
import com.cheapRide.service.UberEstmiateService;;

@Service
public class RideEstimateServiceImpl implements RideEstimateService {

	private final static org.slf4j.Logger logger = LoggerFactory.getLogger(RideEstimateServiceImpl.class);

	private static ObjectMapper mapper = new ObjectMapper();
	

	@Autowired
	LyftEstimateService lyftEstimateService;
	
	@Autowired
	UberEstmiateService uberEstimateService;
	
	@Value("${4_seats}")
	private String seats4;

	@Value("${6_or_more_seats}")
	private String seats6;

	@Value("${luxury_4_seats}")
	private String lux4;
	
	@Value("${share}")
	private String share;


	public String getEstimates(float originLat, float originLong, float destLat, float destLon, Map<String,String> options,String carType) {
		logger.debug("Start : RideEstimateServiceImpl => getEstimates  for origin lattitude" + originLat
				+ " origin longitude " + originLong + " destination lattitude " + destLat + " destination longitude "
				+ destLon);
		String returnVal = null;
		try {
			
			ListUberPriceModel listUberModel = uberEstimateService.getPriceEstmiate(""+originLat, ""+originLong,""+destLat, ""+destLon);
			ListLyftPriceModel listLyftModel = lyftEstimateService.getPriceEstmiate(""+originLat, ""+originLong,""+destLat, ""+destLon);
			ListUberETAModel   listUberETAModel = uberEstimateService.getETA(""+originLat, ""+originLong);
			ListLyftETAModel   listLyftETAModel = lyftEstimateService.getETA(""+originLat, ""+originLong);
			String uberCarType = null;
			String lyftCarType = null;
			if(carType != null){
				String[] carTypeArr = null;;
				if("4_seats".equalsIgnoreCase(carType))
					carTypeArr = seats4.split(":");
				if("6_or_more_seats".equalsIgnoreCase(carType))
					carTypeArr = seats6.split(":");
				if("luxury_4_seats".equalsIgnoreCase(carType))
					carTypeArr = lux4.split(":");
				if("share".equalsIgnoreCase(carType))
					carTypeArr = lux4.split(":");
				uberCarType = carTypeArr[1];
				lyftCarType = carTypeArr[0];
			}
			
			UberPriceModel uberPriceMoel = getCheapMinCostUber(listUberModel, uberCarType);
			UberETAModel	uberETAModel = getCheapMinETAUber(listUberETAModel, uberCarType);
			LyftPriceModel lyftPriceModel = getCheapMinCostLyft(listLyftModel, lyftCarType);
			LyftETAModel	lyftETAModel = getCheapMinETALyft(listLyftETAModel, lyftCarType);
			
			EstimateResponseModel estResModel = getEstResponse(uberPriceMoel, uberETAModel, lyftPriceModel, lyftETAModel);
			
			returnVal = mapper.writeValueAsString(estResModel);
			
		} catch (Exception exc) {
			logger.error("ERROR : RideEstimateServiceImpl => getEstimates  for origin lattitude" + originLat
					+ " origin longitude " + originLong + " destination lattitude " + destLat + " destination longitude "
					+ destLon);
			exc.printStackTrace();
		}
		logger.debug("End : RideEstimateServiceImpl => getEstimates  for origin lattitude" + originLat
				+ " origin longitude " + originLong + " destination lattitude " + destLat + " destination longitude "
				+ destLon);
		System.out.println(returnVal);
		return returnVal;
	}




	private EstimateResponseModel getEstResponse(UberPriceModel uberPriceMoel, UberETAModel uberETAModel,
		LyftPriceModel lyftPriceModel, LyftETAModel lyftETAModel) {
		logger.debug("Start : RideEstimateServiceImpl => getEstResponse ");
		EstimateResponseModel estResModel = new EstimateResponseModel();
		ResponseModel uber = new ResponseModel();
		//uber.setMaxVal(uberPriceMoel.getHigh_estimate());
		if(uberPriceMoel != null){
			uber.setCost(uberPriceMoel.getLow_estimate());
			uber.setRideRequestId(uberPriceMoel.getProduct_id());
			uber.setTime((uberETAModel.getEstimate()/60)+"");
			uber.setRideType(uberPriceMoel.getLocalized_display_name());
		} 
		
		ResponseModel lyft = new ResponseModel();
		//lyft.setMaxVal(lyftPriceModel.getEstimated_cost_cents_max());
		if(lyftPriceModel != null){
			lyft.setCost(lyftPriceModel.getEstimated_cost_cents_min()/100);
			lyft.setRideRequestId(lyftPriceModel.getRide_type());
			lyft.setTime((lyftETAModel.getEta_seconds())/60+"");
			lyft.setRideType(lyftPriceModel.getRide_type());
		}
		
		estResModel.setUber(uber);
		estResModel.setLyft(lyft);
		logger.debug("End : RideEstimateServiceImpl => getEstResponse ");
		return estResModel;
	}




	private UberPriceModel getCheapMinCostUber(ListUberPriceModel listUberModel,String carType) {
		logger.debug("Start : RideEstimateServiceImpl => getCheapMinCostUber ");
		UberPriceModel returnUberPriceModel = null;
		String uberCarType = null;
		if(listUberModel.getPrices().size()> 0){
			//if(options != null)
				uberCarType = carType;// options.get(Contants.UBER_CART_TYPE);
			List<UberPriceModel> lisUberPriceModel = listUberModel.getPrices();
			
			if(uberCarType == null){
				Collections.sort(lisUberPriceModel, new UberMinPriceComparator());
				for(UberPriceModel model : lisUberPriceModel ){
						if(model.getLow_estimate() > 0){
							returnUberPriceModel = model;
							break;
						}
				}
			}else{
				double minPrice = 10000.0;
				for(UberPriceModel model : lisUberPriceModel ){
					if(uberCarType.equalsIgnoreCase(model.getDisplay_name())){
						if(model.getLow_estimate() < minPrice){
							returnUberPriceModel = model;
							minPrice = model.getLow_estimate();
						}
					}
				}
			}
		}
		
		logger.debug("End : RideEstimateServiceImpl => getCheapMinCostUber ");
		return returnUberPriceModel;
	}
	
	private UberETAModel getCheapMinETAUber(ListUberETAModel listUberModel,String carType) {
		logger.debug("Start : RideEstimateServiceImpl => getCheapMinETAUber ");
		String uberCarType = null;
		UberETAModel returnUberEtaModel = null;
		if(listUberModel.getTimes().size()> 0){
		//	if(options != null)
			 uberCarType = carType;//options.get(Contants.UBER_CART_TYPE);
			List<UberETAModel> listUberEtaModel = listUberModel.getTimes();
			if(uberCarType == null){
				Collections.sort(listUberEtaModel, new UberETAComparator());
				returnUberEtaModel = listUberEtaModel.get(0);
			}else{
				double minETA = 10000.0;
				for(UberETAModel model : listUberEtaModel ){
					if(uberCarType.equalsIgnoreCase(model.getDisplay_name())){
						if(model.getEstimate() < minETA){
							returnUberEtaModel = model;
							minETA = model.getEstimate();
						}
					}
				}
			}
		}
		logger.debug("End : RideEstimateServiceImpl => getCheapMinETAUber ");
		return returnUberEtaModel;
	}

	
	private LyftPriceModel getCheapMinCostLyft(ListLyftPriceModel listLyftModel,String carType) {
		logger.debug("Start : RideEstimateServiceImpl => getCheapMinCostLyft ");
		String lyftCarType = null;
		LyftPriceModel returnLyftPriceModel = null;
		if(listLyftModel.getErrorMessage() ==null){
			//if(options != null)
			 lyftCarType = carType;// options.get(Contants.LYFT_CART_TYPE);
			List<LyftPriceModel> listLyftPriceModel = listLyftModel.getCost_estimates();
					
			if(lyftCarType == null){
				Collections.sort(listLyftPriceModel, new LyftMinPriceComparator());
				returnLyftPriceModel = listLyftPriceModel.get(0);
			}else{
				double minPrice = 10000.0;
				for(LyftPriceModel model : listLyftPriceModel ){
					if(lyftCarType.equalsIgnoreCase(model.getRide_type())){
						if(model.getEstimated_cost_cents_min() < minPrice){
							returnLyftPriceModel = model;
							minPrice = model.getEstimated_cost_cents_min();
						}
					}
				}
			}
		}
		logger.debug("End : RideEstimateServiceImpl => getCheapMinCostLyft ");
		return returnLyftPriceModel;
	}

	
	private LyftETAModel getCheapMinETALyft(ListLyftETAModel listLyftModel,String carType) {
		logger.debug("Start : RideEstimateServiceImpl => getCheapMinETALyft ");
		LyftETAModel returnLyftETAModel = null;
		String lyftCarType = null;
		if(listLyftModel.getErrorMessage() == null ){
			//if(options != null)
			 lyftCarType = carType;
			List<LyftETAModel> listLyftETAModel = listLyftModel.getEta_estimates();
			if(lyftCarType == null){
				Collections.sort(listLyftETAModel, new LyftETAComparator());
				returnLyftETAModel = listLyftETAModel.get(0);
			}else{
				double minETA = 10000.0;
				for(LyftETAModel model : listLyftETAModel ){
					if(lyftCarType.equalsIgnoreCase(model.getRide_type())){
						if(model.getEta_seconds() < minETA){
							returnLyftETAModel = model;
							minETA = model.getEta_seconds();
						}
					}
				}
			}
		}
		logger.debug("End : RideEstimateServiceImpl => getCheapMinETALyft ");
		return returnLyftETAModel;
	}



	@Test
	public String getEstimatedTime(float originLat, float originLong, Map<String,String> options) {
		logger.debug("Start : RideEstimateServiceImpl => getEstimatedTime  for origin lattitude" + originLat
				+ " origin longitude " + originLong );
		String returnVal = null;
		try {
			
		} catch (Exception exc) {
			logger.error("ERROR : RideEstimateServiceImpl => getEstimatedTime  for origin lattitude" + originLat
					+ " origin longitude " + originLong);
		}
		logger.debug("Start : RideEstimateServiceImpl => getEstimatedTime  for origin lattitude" + originLat
				+ " origin longitude " + originLong );
		return returnVal;
	}


}
