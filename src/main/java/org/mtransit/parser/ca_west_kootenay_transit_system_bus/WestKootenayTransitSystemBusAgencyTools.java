package org.mtransit.parser.ca_west_kootenay_transit_system_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

import static org.mtransit.parser.StringUtils.EMPTY;

// https://www.bctransit.com/open-data
// https://www.bctransit.com/data/gtfs/west-kootenay.zip
public class WestKootenayTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(@Nullable String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-west-kootenay-transit-system-bus-android/res/raw/";
			args[2] = ""; // files-prefix
		}
		new WestKootenayTransitSystemBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<Integer> serviceIdInts;

	@Override
	public void start(@NotNull String[] args) {
		MTLog.log("Generating West Kootenay Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIdInts = extractUsefulServiceIdInts(args, this, true);
		super.start(args);
		MTLog.log("Generating West Kootenay Transit System bus data... DONE in %s.", Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIdInts != null && this.serviceIdInts.isEmpty();
	}

	@Override
	public boolean excludeCalendar(@NotNull GCalendar gCalendar) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarInt(gCalendar, this.serviceIdInts);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(@NotNull GCalendarDate gCalendarDates) {
		if (this.serviceIdInts != null) {
			return excludeUselessCalendarDateInt(gCalendarDates, this.serviceIdInts);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "11"; // West Kootenay Transit System only

	@Override
	public boolean excludeRoute(@NotNull GRoute gRoute) {
		//noinspection deprecation
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(@NotNull GTrip gTrip) {
		if (this.serviceIdInts != null) {
			return excludeUselessTripInt(gTrip, this.serviceIdInts);
		}
		return super.excludeTrip(gTrip);
	}

	@NotNull
	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(@NotNull GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	// private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@NotNull
	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Nullable
	@Override
	public String getRouteColor(@NotNull GRoute gRoute) {
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
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final String SLASH = " / ";
	private static final String COMMUNITY_COMPLEX = "Comm Complex";
	private static final String TRAIL = "Trail";

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;

	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		//noinspection deprecation
		map2.put(1L, new RouteTripSpec(1L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", // North
				1, MTrip.HEADSIGN_TYPE_STRING, "Uphill") // South
				.addTripSort(0, //
						Arrays.asList(//
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
								"160376" // == Northbound Ward St at Baker St (Downtown Nelson)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160292", // ==
								"160300", // != =>
								"160293", // !=
								"160297" // Southbound Stanley St at Hart St #HS =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(2L, new RouteTripSpec(2L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", // West
				1, MTrip.HEADSIGN_TYPE_STRING, "Fairview") // East
				.addTripSort(0, //
						Arrays.asList(//
								"160359", // == 10th St at Kokanee (Selkirk College) <=
								"160013", // != 11th St
								"160339", // == Fell St at 9th St <=
								"160338", // == 8th St at Kokanee
								"160335", // == 8th St at Davies St
								"160324", // != Cottonwood St at 7th St (LV Rogers Secondary School)
								"160327", // != 5th St at Davies St
								"160395", // Gordon at Lakeside #NR
								"160392", // == 1st St at Anderson St
								"160318", // != xx Nelson at Behnsen St
								"160326", // != xx Cottonwood St at 3rd St
								"160341", // != == Hendryx St at Mill St
								"160301", // != Latimer St at Josephine St
								"160014", // != Hoover at Hall
								"160515", // == Front at Poplar
								"160378", // != xx Lakeside at Poplar St (Chahko Miko Mall)
								"160351", // == Hall St at Front St
								"160304", // != Robson St at Hendryx St
								"160281", // != Josephine St at Hoover St
								"160376" // == Ward St at Baker St (Downtown Nelson)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // == Ward St at Baker St (Downtown Nelson)
								"160350", // == Hall St at Front St
								"160378", // != xx Lakeside at Poplar St (Chahko Miko Mall)
								"160342", // == Front St at Poplar St
								"160318", // != xx Nelson at Behnsen St
								"160326", // != xx Cottonwood St at 3rd St
								"160325", // != == Cottonwood St at 5th St
								"160323", // != Cottonwood St at 7th St (LV Rogers Secondary School)
								"160391", // != 1st St at Anderson St
								"160329", // != Gordon St at 3rd St
								"160336", // == 8th St at Davies St
								"160359" // == 10th St at Kokanee (Selkirk College) =>
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(3L, new RouteTripSpec(3L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Rosemont") //
				.addTripSort(0, //
						Arrays.asList(//
								"160311", // Northbound Silver King at Tower (Selkirk College)
								"160302", // == Northbound Hall Mines at Hoover St
								"160291", // !=
								"160376", // Northbound Ward St at Baker St =>
								"160293", // !=
								"160297" // Southbound Stanley St at Hart St #HS =>
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160279", // ==
								"160278", // !=
								"160275", // Westbound W Innes St at Crease #CI
								"160368", // !=
								"160310", // ==
								"160311" // Northbound Silver King at Tower (Selkirk College)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(4L, new RouteTripSpec(4L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Nelson Airport") //
				.addTripSort(0, //
						Arrays.asList(//
								"160317", // Eastbound Lakeside Dr at Airport
								"560005", // ++
								"160376" // Northbound Ward St at Baker St
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // Northbound Ward St at Baker St
								"560082", // ++
								"160379" // Westbound Lakeside Dr at Airport
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(14L, new RouteTripSpec(14L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Blewett") //
				.addTripSort(0, //
						Arrays.asList(//
								"560011", // Blewett at Marrello
								"560014", // ++
								"160376" // Northbound Ward St at Baker St (Downtown Nelson)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160282", // ==
								"160511", // !=
								"160462", // == Westbound Hwy 3A/6 at 1600 Block
								"560093", // != Southbound Hwy 3A & 6 at Pacific Insight Corp
								"560008", // != Westbound Granite at Blewett
								"560009", // == Westbound Blewett at Bedford
								"560011" // Blewett at Marrello
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(15L, new RouteTripSpec(15L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Perrier") //
				.addTripSort(0, //
						Arrays.asList(//
								"560015", // Southbound Perrier Rd
								"160291", // ++
								"160376" // Northbound Ward St at Baker St (Downtown Nelson)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160376", // Northbound Ward St at Baker St (Downtown Nelson)
								"160292", // ++
								"560015" // Southbound Perrier Rd
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(34L, new RouteTripSpec(34L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COMMUNITY_COMPLEX, //
				1, MTrip.HEADSIGN_TYPE_STRING, "Kinnaird") //
				.addTripSort(0, //
						Arrays.asList(//
								"160246", // Northbound 14th at Meadowbrook #KinnairdPark
								"160268", // ++
								"160247" // Northbound 6th at 20th St #CommComplex
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160247", // Northbound 6th at 20th St #CommComplex
								"160252", // == Southbound 5th at 32nd St
								"160266", // != Southbound 9th at 35th St #SouthRdg
								"160028", // != Southbound 14th at 37th St #SouthRdg
								"160260", // == Northbound Columbia at 32nd St
								"160246" // Northbound 14th at Meadowbrook #KinnairdPark
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(36L, new RouteTripSpec(36L, //
				0, MTrip.HEADSIGN_TYPE_STRING, COMMUNITY_COMPLEX, //
				1, MTrip.HEADSIGN_TYPE_STRING, "Bridgeview Cr") //
				.addTripSort(0, //
						Arrays.asList(//
								"160243", // Southbound Bridgeview at Lawrence
								"160390", // ==
								"160002", // != Westbound Selkirk College =>
								"160348", // !=
								"160247" // != Northbound 6th at 20th St #CommComplex =>
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160247", // != Northbound 6th at 20th St #CommComplex <=
								"160002", // != Westbound Selkirk College <=
								"160257", // !=
								"160242", // ==
								"160243" // Southbound Bridgeview at Lawrence
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(38L, new RouteTripSpec(38L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Downtown", // Castlegar: City Hall
				1, MTrip.HEADSIGN_TYPE_STRING, "Playmor") //
				.addTripSort(0, //
						Arrays.asList(//
								"160468", // Osachoff at White #PlaymorJunction
								"160470", // Hwy 3A at Kelly
								"160384", // Hwy 3A at Rosedale
								"160015", // Columbia 1500 block
								"160221" // Columbia at 4th St #Downtown
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160221", // Columbia at 4th St
								"160562", // Hwy 6 at 1200 Block
								"160468" // Osachoff at White #PlaymorJunction
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(41L, new RouteTripSpec(41L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Cedar & Spokane", // Downtown
				1, MTrip.HEADSIGN_TYPE_STRING, "Nelson & Birch") //
				.addTripSort(0, //
						Arrays.asList(//
								"160024", // Nelson at Lookout St (Nelson and Birch)
								"160025", // ++
								"160087" // Northbound Cedar at Spokane St
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160087", // Northbound Cedar at Spokane St
								"160080", // ++
								"160024" // Nelson at Lookout St (Nelson and Birch)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(42L, new RouteTripSpec(42L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Cedar & Spokane", // Downtown
				1, MTrip.HEADSIGN_TYPE_STRING, "Daniel & End") //
				.addTripSort(0, //
						Arrays.asList(//
								"160092", // Westbound Daniel St at End
								"160085", // ++
								"160087" // Northbound Cedar at Spokane St
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160087", // Northbound Cedar at Spokane St
								"160096", // ++
								"160092" // Westbound Daniel St at End
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(44L, new RouteTripSpec(44L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Cedar & Spokane", // Downtown
				1, MTrip.HEADSIGN_TYPE_STRING, "Sunningdale") // KBR Hospital
				.addTripSort(0, //
						Arrays.asList(//
								"160148", // Northbound Hillside at Viola
								"160109", // != ==
								"160041", // <>
								"160048", // <>
								"160042", // <> Eastbound Hospital Bench 1200 Block
								"160117", // <>
								"160107", // != ==
								"160087" // Northbound Cedar at Spokane St
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160087", // Northbound Cedar at Spokane St
								"160106", // !=
								"160041", // <>
								"160048", // <>
								"160042", // <> Eastbound Hospital Bench 1200 Block
								"160117", // <>
								"160108", // !=
								"160148" // Northbound Hillside at Viola
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(45L, new RouteTripSpec(45L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Cedar & Spokane", // Downtown
				1, MTrip.HEADSIGN_TYPE_STRING, "Warfield Plant") // Teck
				.addTripSort(0, //
						Arrays.asList(//
								"160030", // == Warfield Plant
								"560140", // != <> Teck
								"160140", // != <>
								"160025", // ==
								"160087" // Northbound Cedar at Spokane St
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160087", // Northbound Cedar at Spokane St
								"160022", // ==
								"560140", // <> Teck
								"160140", // <>
								"160030" // == Warfield Plant
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(48L, new RouteTripSpec(48L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Fruitvale", // Trail / Fruitvale
				1, MTrip.HEADSIGN_TYPE_STRING, "Red Mountain") //
				.addTripSort(0, //
						Arrays.asList(//
								"160038", // Red Mountain Resort
								"160130", // ++
								"160158" // Westbound Main St at Kootenay Ave S
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160031", // Northbound Kootenay Ave N at Pole Yard
								"160188", // ++
								"160038" // Red Mountain Resort
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(51L, new RouteTripSpec(51L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Nakusp", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Hot Spgs") //
				.addTripSort(0, //
						Arrays.asList(//
								"560050", // Northbound Hotsprings Rd
								"160206", // Southbound 6th W at 2nd E
								"160207" // Eastbound Broadway St W at 7th Ave SW
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"160207", // Eastbound Broadway St W at 7th Ave SW
								"160207", // ++
								"560050", // ++
								"560050" // Northbound Hotsprings Rd
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(57L, new RouteTripSpec(57L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Seniors Hall", // Kaslo
				1, MTrip.HEADSIGN_TYPE_STRING, "Shutty Bench") //
				.addTripSort(0, //
						Arrays.asList(//
								"560022", // Northbound Hwy 31 at Shutty Bench Rd (Shutty Bench)
								"560023", // ++
								"560021" // Westbound A Ave (Seniors Hall)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"560021", // Westbound A Ave (Seniors Hall)
								"560021", // ++
								"560022", // ++
								"560022" // Northbound Hwy 31 at Shutty Bench Rd (Shutty Bench)
						)) //
				.compileBothTripSort());
		//noinspection deprecation
		map2.put(58L, new RouteTripSpec(58L, //
				0, MTrip.HEADSIGN_TYPE_STRING, "Kaslo", //
				1, MTrip.HEADSIGN_TYPE_STRING, "Argenta") //
				.addTripSort(0, //
						Arrays.asList(//
								"560048", // Highway 31 at Duncan Dam Lookout Rd (Argenta)
								"560067", // ==
								"560069", // != <> Southbound Argenta Johnsons Landing Rd
								"560088", // ==
								"560021" // Westbound A Ave (Kaslo)
						)) //
				.addTripSort(1, //
						Arrays.asList(//
								"560021", // Westbound A Ave (Kaslo)
								"560022", // ==
								"560069", // != <> Southbound Argenta Johnsons Landing Rd
								"560068", // ==
								"560048" // Highway 31 at Duncan Dam Lookout Rd (Argenta)
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, @NotNull List<MTripStop> list1, @NotNull List<MTripStop> list2, @NotNull MTripStop ts1, @NotNull MTripStop ts2, @NotNull GStop ts1GStop, @NotNull GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@NotNull
	@Override
	public ArrayList<MTrip> splitTrip(@NotNull MRoute mRoute, @Nullable GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@NotNull
	@Override
	public Pair<Long[], Integer[]> splitTripStop(@NotNull MRoute mRoute, @NotNull GTrip gTrip, @NotNull GTripStop gTripStop, @NotNull ArrayList<MTrip> splitTrips, @NotNull GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	private static final Pattern ENDS_WITH_SLASH_VIA_ = Pattern.compile("( / via .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RLN_SLASH_ = Pattern.compile("(^.* / )", Pattern.CASE_INSENSITIVE);

	private static final Pattern KEEP_TRAIL_ = CleanUtils.cleanWords("trl");
	private static final String KEEP_TRAIL_REPLACEMENT = CleanUtils.cleanWordsReplacement(TRAIL);

	@NotNull
	@Override
	public String cleanTripHeadsign(@NotNull String tripHeadsign) {
		tripHeadsign = ENDS_WITH_SLASH_VIA_.matcher(tripHeadsign).replaceAll(EMPTY); // 1st - remove via
		tripHeadsign = STARTS_WITH_RLN_SLASH_.matcher(tripHeadsign).replaceAll(EMPTY); // 2nd - remove rln
		tripHeadsign = CleanUtils.keepToAndRemoveVia(tripHeadsign);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign); // 1st
		tripHeadsign = KEEP_TRAIL_.matcher(tripHeadsign).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(@NotNull MTrip mTrip, @NotNull MTrip mTripToMerge) {
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
					"31 " + COMMUNITY_COMPLEX, //
					COMMUNITY_COMPLEX //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COMMUNITY_COMPLEX, mTrip.getHeadsignId());
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
					COMMUNITY_COMPLEX //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString(COMMUNITY_COMPLEX, mTrip.getHeadsignId());
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
					COMMUNITY_COMPLEX, //
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
				mTrip.setHeadsignString(TRAIL, mTrip.getHeadsignId()); // DOWNTOWN (Cedar & Spokane)
				return true;
			} else if (Arrays.asList( //
					"Waneta" + SLASH + "Walmart", //
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
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	private static final Pattern STARS_ = Pattern.compile("(\\*\\*(.*)\\*\\*)", Pattern.CASE_INSENSITIVE);
	private static final String STARS_REPLACEMENT = "($2)";

	@NotNull
	@Override
	public String cleanStopName(@NotNull String gStopName) {
		gStopName = STARS_.matcher(gStopName).replaceAll(STARS_REPLACEMENT);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName); // 1st
		gStopName = KEEP_TRAIL_.matcher(gStopName).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
