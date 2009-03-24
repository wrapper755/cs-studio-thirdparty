package org.csstudio.sds.components.ui.internal.editparts;

import org.csstudio.sds.components.model.AbstractScaledWidgetModel;
import org.csstudio.sds.components.ui.internal.figures.AbstractScaledWidgetFigure;
import org.csstudio.sds.ui.editparts.AbstractBaseEditPart;
import org.csstudio.sds.ui.editparts.AbstractWidgetEditPart;
import org.csstudio.sds.ui.editparts.IWidgetPropertyChangeHandler;
import org.eclipse.draw2d.IFigure;

/**
 * Base editPart controller for a widget based on {@link AbstractScaledWidgetModel}.
 * 
 * @author Xihui Chen
 * 
 */
public abstract class AbstractScaledWidgetEditPart extends AbstractWidgetEditPart {

	/**
	 * Sets those properties on the figure that are defined in the
	 * {@link AbstractScaledWidgetFigure} base class. This method is provided for the
	 * convenience of subclasses, which can call this method in their
	 * implementation of {@link AbstractBaseEditPart#doCreateFigure()}.
	 * 
	 * @param figure
	 *            the figure.
	 * @param model
	 *            the model.
	 */
	protected final void initializeCommonFigureProperties(
			final AbstractScaledWidgetFigure figure, final AbstractScaledWidgetModel model) {
		
		figure.setValue(model.getValue());
		figure.setMinimum(model.getMinimum());
		figure.setMaximum(model.getMaximum());
		figure.setLogScale(model.isLogScaleEnabled());
		figure.setShowScale(model.isShowScale());
		figure.setShowMinorTicks(model.isShowMinorTicks());
		figure.setShowMarkers(model.isShowMarkers());
		figure.setLoloLevel(model.getLoloLevel());
		figure.setLoLevel(model.getLoLevel());
		figure.setHiLevel(model.getHiLevel());
		figure.setHihiLevel(model.getHihiLevel());
		figure.setTransparent(model.isTransparent());
		
	}	
	
	/**
	 * Registers property change handlers for the properties defined in
	 * {@link AbstractScaledWidgetModel}. This method is provided for the convenience
	 * of subclasses, which can call this method in their implementation of
	 * {@link #registerPropertyChangeHandlers()}.
	 */
	protected final void registerCommonPropertyChangeHandlers() {
		// value
		IWidgetPropertyChangeHandler valueHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setValue((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_VALUE, valueHandler);
		
		//minimum
		IWidgetPropertyChangeHandler minimumHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setMinimum((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_MIN, minimumHandler);
		
		//maximum
		IWidgetPropertyChangeHandler maximumHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setMaximum((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_MAX, maximumHandler);
		
		//logScale
		IWidgetPropertyChangeHandler logScaleHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setLogScale((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_LOG_SCALE, logScaleHandler);
		
		//showScale
		IWidgetPropertyChangeHandler showScaleHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setShowScale((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_SHOW_SCALE, showScaleHandler);
		
		//showMarkers
		IWidgetPropertyChangeHandler showMarkersHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setShowMarkers((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_SHOW_MARKERS, showMarkersHandler);
		
		//showMinorTicks
		IWidgetPropertyChangeHandler showMinorTicksHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setShowMinorTicks((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_SHOW_MINOR_TICKS, showMinorTicksHandler);
		
		//LoLo Level
		IWidgetPropertyChangeHandler loloHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setLoloLevel((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_LOLO_LEVEL, loloHandler);

		//Lo Level
		IWidgetPropertyChangeHandler loHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setLoLevel((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_LO_LEVEL, loHandler);
		
		//Hi Level
		IWidgetPropertyChangeHandler hiHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setHiLevel((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_HI_LEVEL, hiHandler);
		
		//HiHi Level
		IWidgetPropertyChangeHandler hihiHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setHihiLevel((Double) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_HIHI_LEVEL, hihiHandler);
		
		//Transparent
		IWidgetPropertyChangeHandler transparentHandler = new IWidgetPropertyChangeHandler() {
			public boolean handleChange(final Object oldValue,
					final Object newValue,
					final IFigure refreshableFigure) {
				AbstractScaledWidgetFigure figure = (AbstractScaledWidgetFigure) refreshableFigure;
				figure.setTransparent((Boolean) newValue);
				return true;
			}
		};
		setPropertyChangeHandler(AbstractScaledWidgetModel.PROP_TRANSPARENT, transparentHandler);
		
	}

}
