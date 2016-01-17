package de.rennschnitzel.net.router.util;

import java.util.function.DoubleUnaryOperator;

public abstract class TorqueFunction {

  /**
   * Gets the torque of a stepper motor
   * 
   * @param pulseSpeed - step speed in Hz
   * @return torque - in Nm (kg*m²/s²)
   */
  public abstract double torque(double pulseSpeed);

  /**
   * Gets the torque-velocity curve of the motor in Nm*s (kg*m²/s)
   * 
   * @return integral of torque function
   */
  public abstract DoubleUnaryOperator integrate();

  /**
   * 
   * @param pulseSpeed - step speed in Hz
   * @param momentOfInertia - inertia in kg*m² ( must be greater than zero )
   * @return acceleration - that the motor can do in step/s²
   */
  public double acceleration(double pulseSpeed, double momentOfInertia) {
    return this.torque(pulseSpeed) / momentOfInertia;
  }

  /**
   * 
   * @param pulseSpeed - step speed in Hz
   * @param momentOfInertia - inertia in kg*m² ( must be greater than zero )
   * @return time to stop - in seconds (s)
   */
  public double timeToStop(double pulseSpeed, double momentOfInertia) {
    double acceleration = this.acceleration(pulseSpeed, momentOfInertia);
    double velocity = integrate().applyAsDouble(pulseSpeed) / momentOfInertia;
    return (velocity / acceleration);
  }
}
