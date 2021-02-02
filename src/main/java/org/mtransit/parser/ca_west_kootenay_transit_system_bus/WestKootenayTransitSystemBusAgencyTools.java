package org.mtransit.parser.ca_west_kootenay_transit_system_bus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;

import java.util.HashSet;
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

	@Override
	public void setTripHeadsign(@NotNull MRoute mRoute, @NotNull MTrip mTrip, @NotNull GTrip gTrip, @NotNull GSpec gtfs) {
		mTrip.setHeadsignString(
				cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
				gTrip.getDirectionIdOrDefault()
		);
	}

	@Override
	public boolean directionFinderEnabled() {
		return true;
	}

	private static final Pattern ENDS_WITH_SLASH_VIA_ = Pattern.compile("( / via .*$)", Pattern.CASE_INSENSITIVE);

	private static final Pattern STARTS_WITH_RLN_SLASH_ = Pattern.compile("(^.* / )", Pattern.CASE_INSENSITIVE);

	private static final String TRAIL = "Trail";
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
