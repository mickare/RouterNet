package de.rennschnitzel.backbone.router.util;

import java.util.function.Function;

public enum EASING implements Function<Double, Double> {
	LINEAR( p -> p ),
	SWING( p -> 0.5 - Math.cos( p * Math.PI ) / 2 ),
	QUAD_IN( p -> Math.pow( p, 2 ) ),
	QUAD_OUT( p -> 1 - EASING.QUAD_IN.apply( 1 - p ) ),
	QUAD_INOUT( p -> p < 0.5 ? ( EASING.QUAD_IN.apply( p * 2 ) / 2 ) : ( 1 - EASING.QUAD_IN.apply( p * -2 + 2 ) / 2 ) ),
	SINE_IN( p -> 1 - Math.cos( p * Math.PI / 2 ) ),
	SINE_OUT( p -> 1 - EASING.SINE_IN.apply( 1 - p ) ),
	SINE_INOUT( p -> p < 0.5 ? ( EASING.SINE_IN.apply( p * 2 ) / 2 ) : ( 1 - EASING.SINE_IN.apply( p * -2 + 2 ) / 2 ) ),
	CIRC_IN( p -> 1 - Math.sqrt( 1 - p * p ) ),
	CIRC_OUT( p -> 1 - EASING.CIRC_IN.apply( 1 - p ) ),
	CIRC_INOUT( p -> p < 0.5 ? ( EASING.CIRC_IN.apply( p * 2 ) / 2 ) : ( 1 - EASING.CIRC_IN.apply( p * -2 + 2 ) / 2 ) ),
	ELASTIC_IN( p -> p == 0 || p == 1 ? p : -Math.pow( 2, 8 * ( p - 1 ) )
			* Math.sin( ( ( p - 1 ) * 80 - 7.5 ) * Math.PI / 15 ) ),
	ELASTIC_OUT( p -> 1 - EASING.ELASTIC_IN.apply( 1 - p ) ),
	ELASTIC_INOUT( p -> p < 0.5 ? ( EASING.ELASTIC_IN.apply( p * 2 ) / 2 )
			: ( 1 - EASING.ELASTIC_IN.apply( p * -2 + 2 ) / 2 ) ),
	BACK_IN( p -> p * p * ( 3 * p - 2 ) ),
	BACK_OUT( p -> 1 - EASING.BACK_IN.apply( 1 - p ) ),
	BACK_INOUT( p -> p < 0.5 ? ( EASING.BACK_IN.apply( p * 2 ) / 2 ) : ( 1 - EASING.BACK_IN.apply( p * -2 + 2 ) / 2 ) ),
	BOUNCE_IN( p -> {
		double pow2;
		int bounce = 4;
		while ( p < ( ( pow2 = Math.pow( 2, --bounce ) ) - 1 ) / 11 ) {
		}
		return 1 / Math.pow( 4, 3 - bounce ) - 7.5625 * Math.pow( ( pow2 * 3 - 2 ) / 22 - p, 2 );
	} ),
	BOUNCE_OUT( p -> 1 - EASING.BOUNCE_IN.apply( 1 - p ) ),
	BOUNCE_INOUT( p -> p < 0.5 ? ( EASING.BOUNCE_IN.apply( p * 2 ) / 2 )
			: ( 1 - EASING.BOUNCE_IN.apply( p * -2 + 2 ) / 2 ) );
	
	private final Function<Double, Double> func;
	
	private EASING( Function<Double, Double> func ) {
		this.func = func;
	}
	
	public final Double apply( final Double p ) {
		return func.apply( p );
	}
	
	public final double apply( final double p ) {
		return func.apply( p );
	}
}