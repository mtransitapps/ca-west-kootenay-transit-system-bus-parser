package org.mtransit.parser.ca_west_kootenay_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

// https://bctransit.com/*/footer/open-data
// https://bctransit.com/servlet/bctransit/data/GTFS - West Kootenay
public class WestKootenayTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-west-kootenay-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WestKootenayTransitSystemBusAgencyTools().start(args);
	}

	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		System.out.printf("\nGenerating West Kootenay Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		System.out.printf("\nGenerating West Kootenay Transit System bus data... DONE in %s.\n", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "11"; // West Kootenay Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	@SuppressWarnings("unused")
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 1: return "0C4E8C";
			case 2: return "8EC641";
			case 3: return "F78B21";
			case 4: return "8177B7";
			case 10: return "E8C734";
			case 14: return "F399C0";
			case 15: return "BC4F67";
			case 20: return "29ABE2";
			case 31: return "FAA74A";
			case 32: return "90288C";
			case 33: return "B3AA7E";
			case 34: return "76AD99";
			case 36: return "875E9F";
			case 38: return "5E86A0";
			case 41: return "00B5AD";
			case 42: return "7C3F24";
			case 43: return "DC7126";
			case 44: return "05A84D";
			case 45: return "8178B8";
			case 46: return "BF83B9";
			case 47: return "AF6F29";
			case 48: return "056937";
			case 51: return "E370AB";
			case 52: return "B1BB35";
			case 53: return "0073AF";
			case 57: return "8D173C";
			case 58: return "EC1A8D";
			case 72: return "A54399";
			case 74: return "AF6E0E";
			case 76: return "4F6F19";
			case 98: return "4D4D4F";
			case 99: return "5D86A0";
			// @formatter:on
			}
			System.out.printf("\nUnexpected route color for %s!\n", gRoute);
			System.exit(-1);
			return null;
		}
		return super.getRouteColor(gRoute);
	}

	private static final String TRAIL = "Trail";

	private static HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<Long, RouteTripSpec>();
		map2.put(1L, new RouteTripSpec(1L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", // North
				1, MTrip.HEADSIGN_TYPE_STRING, "Uphill") // South
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"160297", // Southbound Stanley St at Hart St #HS <=
								"160305", // Westbound Robson St at Josephine St (Trafalgar Middle School)
								"160281", // ==
								"160300", // <=
								"160340", // ==
								"160313", // == Westbound View St at Pine St (Kootenay Lake Hospital)
								"160341", // != Southbound Hendryx St at Mill St
								"160301", // != Westbound Latimer St at Josephine St
								"160325", // != Eastbound Cottonwood St at 5th St
								"160323", // != Eastbound Cottonwood St at 7th St
								"160376", // == Northbound Ward St at Baker St (Downtown Nelson)
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160292", // ==
								"160300", // != =>
								"160293", // !=
								"160297", // Southbound Stanley St at Hart St #HS =>
						})) //
				.compileBothTripSort());
		map2.put(2L, new RouteTripSpec(2L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", // West
				1, MTrip.HEADSIGN_TYPE_STRING, "Fairview") // East
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"160359", // == Northbound 10th St at Kokanee (Selkirk College) <=
								"160339", // == Westbound Fell St at 9th St <=
								"160338", // ==
								"160335", // ==
								"160324", // != Westbound Cottonwood St at 7th St (LV Rogers Secondary School)
								"160327", // !=
								"160395", // Southbound Gordon at Lakeside #NR
								"160515", // ==
								"160378", // != xx Eastbound Lakeside at Poplar St (Chahko Miko Mall)
								"160351", // ==
								"160376", // == Northbound Ward St at Baker St (Downtown Nelson)
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"160376", // == Northbound Ward St at Baker St (Downtown Nelson)
								"160358", // ==
								"160350", // ==
								"160378", // != xx Eastbound Lakeside at Poplar St (Chahko Miko Mall)
								"160342", // == Northbound Front St at Poplar St
								"160318", // != Northbound Nelson at Behnsen St
								"160323", // != Eastbound Cottonwood St at 7th St (LV Rogers Secondary School)
								"160391", // != Northbound 1st St at Anderson St
								"160329", // != Eastbound Gordon St at 3rd St
								"160336", // == Northbound 8th St at Davies St
								"160359", // == Northbound 10th St at Kokanee (Selkirk College) =>
								"160013", // != Eastbound 11th St (Mountain Lake Seniors Community Ltd) =>
						})) //
				.compileBothTripSort());
		map2.put(3L, new RouteTripSpec(3L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Rosemont") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"160311", // Northbound Silver King at Tower (Selkirk College)
								"160302", // == Northbound Hall Mines at Hoover St
								"160291", // !=
								"160376", // Northbound Ward St at Baker St =>
								"160293", // !=
								"160297", // Southbound Stanley St at Hart St #HS =>
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160279", // ==
								"160278", // !=
								"160275", // Westbound W Innes St at Crease #CI
								"160368", // !=
								"160310", // ==
								"160311", // Northbound Silver King at Tower (Selkirk College)
						})) //
				.compileBothTripSort());
		map2.put(4L, new RouteTripSpec(4L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Nelson Airport") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"160317", // Eastbound Lakeside Dr at Airport
								"560005", // ++
								"160376", // Northbound Ward St at Baker St
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"160376", // Northbound Ward St at Baker St
								"560082", // ++
								"160379", // Westbound Lakeside Dr at Airport
						})) //
				.compileBothTripSort());
		map2.put(48L, new RouteTripSpec(48L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Fruitvale", // Trail / Fruitvale
				1, MTrip.HEADSIGN_TYPE_STRING, "Red Mountain") //
				.addTripSort(0, //
						Arrays.asList(new String[] { //
						"160038", // Red Mountain Resort
								"160130", // ++
								"160158", // Westbound Main St at Kootenay Ave S
						})) //
				.addTripSort(1, //
						Arrays.asList(new String[] { //
						"160031", // Northbound Kootenay Ave N at Pole Yard
								"160188", // ++
								"160038", // Red Mountain Resort
						})) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(cleanTripHeadsign(gTrip.getTripHeadsign()), gTrip.getDirectionId());
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE = Pattern.compile("((^|\\W){1}(exchange)(\\W|$){1})", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " (";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") ";

	private static final Pattern STARTS_WITH_VIA_SLASH = Pattern.compile("(( \\/)? via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO_SLASH = Pattern.compile("(^.* \\/ (to )?)", Pattern.CASE_INSENSITIVE);

	private static final Pattern KEEP_TRAIL = Pattern.compile(String.format("((^|\\W){1}(%s)(\\W|$){1})", "trl"), Pattern.CASE_INSENSITIVE);
	private static final String KEEP_TRAIL_REPLACEMENT = String.format("$2%s$4", TRAIL);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = STARTS_WITH_VIA_SLASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // 1st
		tripHeadsign = STARTS_WITH_TO_SLASH.matcher(tripHeadsign).replaceAll(StringUtils.EMPTY); // 2nd
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CLEAN_P1.matcher(tripHeadsign).replaceAll(CLEAN_P1_REPLACEMENT);
		tripHeadsign = CLEAN_P2.matcher(tripHeadsign).replaceAll(CLEAN_P2_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign); // 1st
		tripHeadsign = KEEP_TRAIL.matcher(tripHeadsign).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 10L) {
			if (Arrays.asList( //
					"6 Mile Only", //
					"Balfour" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Balfour", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 20L) {
			if (Arrays.asList( //
					"Slocan & Perry's", //
					"Slocan City" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Slocan City", mTrip.getHeadsignId()); // SLOCAN VALLEY
				return true;
			} else if (Arrays.asList( //
					"Playmor & Perry's", //
					"Playmor Jct" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Playmor Jct", mTrip.getHeadsignId()); // NELSON
				return true;
			}
		} else if (mTrip.getRouteId() == 31L) {
			if (Arrays.asList( //
					"32 Columbia", //
					"Downtown" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Downtown", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 32L) {
			if (Arrays.asList( //
					"31 N Castlegar", //
					"31 Comm Complex", //
					"Comm Complex" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Comm Complex", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"Celgar Only", //
					"Celgar & Robson", //
					"Robson" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Robson", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 33L) {
			if (Arrays.asList( //
					"98 " + TRAIL, //
					"Comm Complex" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Comm Complex", mTrip.getHeadsignId());
				return true;
			} else if (Arrays.asList( //
					"36 Ootischenia", //
					"Selkirk Coll" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Selkirk Coll", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 36L) {
			if (Arrays.asList( //
					"Selkirk Coll", //
					"Comm Complex", //
					"Ootischenia" // ++
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Ootischenia", mTrip.getHeadsignId()); // Comm Complex / Selkirk Coll
				return true;
			}
		} else if (mTrip.getRouteId() == 43L) {
			if (Arrays.asList( //
					"Waneta Only", //
					TRAIL //
					).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(TRAIL, mTrip.getHeadsignId()); // DOWNTOWN
				return true;
			} else if (Arrays.asList( //
					"Waneta/Walmart", //
					"Fruitvale" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Fruitvale", mTrip.getHeadsignId()); // GLENMERRY/FRUITVALE
				return true;
			}
		} else if (mTrip.getRouteId() == 44L) {
			if (Arrays.asList( //
					"KBR Hosp Only", //
					TRAIL, //
					"Sunningdale" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Sunningdale", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 76L) {
			if (Arrays.asList( //
					"Balfour Only", //
					"Nelson" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Nelson", mTrip.getHeadsignId());
				return true;
			}
		} else if (mTrip.getRouteId() == 99L) {
			if (Arrays.asList( //
					"20 Slocan Vly", //
					"Playmor Jct", //
					"Selk Coll - C'gar" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Selk Coll - C'gar", mTrip.getHeadsignId());
				return true;
			}
		}
		System.out.printf("\nUnexpected trips to merge %s & %s!\n", mTrip, mTripToMerge);
		System.exit(-1);
		return false;
	}

	private static final Pattern STARTS_WITH_BOUND = Pattern.compile("(^(east|west|north|south)bound)", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = STARTS_WITH_BOUND.matcher(gStopName).replaceAll(StringUtils.EMPTY);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName); // 1st
		gStopName = KEEP_TRAIL.matcher(gStopName).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
