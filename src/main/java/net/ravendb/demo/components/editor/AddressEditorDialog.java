package net.ravendb.demo.components.editor;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;

import net.ravendb.client.exceptions.ConcurrencyException;
import net.ravendb.demo.model.Address;
import net.ravendb.demo.model.Patient;
import net.ravendb.demo.presenters.ViewListener;

public class AddressEditorDialog extends AbstractEditorDialog<Patient> {

    private ViewListener.PatientViewListener presenter;
    private Binder<Address> addbinder;
    private Patient bean;
    private ComboBox<String> region;

    public AddressEditorDialog(String title, Patient bean, ViewListener.PatientViewListener presenter) {
        super(title, bean);
        this.presenter = presenter;
        this.bean = bean;
        if (bean.getAddress() == null) {
            bean.setAddress(new Address());
        }
        region.setItems(presenter.getRegionsList());
        this.addbinder.setBean(bean.getAddress());
    }

    @Override
    protected Component buildFormContent() {

        this.addbinder = new Binder<>();
        FormLayout layout = new FormLayout();

        TextField city = new TextField();

        addbinder.forField(city)
                 .asRequired()
                 .bind(Address::getCity, Address::setCity);
        layout.addFormItem(city, "City");

        TextField line =
                new TextField();
        line.setRequiredIndicatorVisible(true);
        addbinder.forField(line)
                 .asRequired()
                 .bind(Address::getLine, Address::setLine);
        layout.addFormItem(line, "Address");

        region =
                new ComboBox<>();

        addbinder.forField(region)
                 .asRequired()
                 .bind(Address::getRegion, Address::setRegion);
        layout.addFormItem(region, "Region");

        TextField code = new TextField();
        addbinder.forField(code).bind(Address::getCode, Address::setCode);
        layout.addFormItem(code, "Code");

        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        return layout;
    }


    @Override
    protected void save(ClickEvent<Button> e) {
        try {
            presenter.save(bean.getId(), addbinder.getBean());
        } catch (ConcurrencyException ce) {
            Notification.show("Document was updated by another user", 5000,
                               Notification.Position.TOP_CENTER);
        }
        this.close();
    }

}
