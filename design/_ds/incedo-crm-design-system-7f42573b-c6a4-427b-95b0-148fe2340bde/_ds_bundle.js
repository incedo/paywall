/* @ds-bundle: {"format":3,"namespace":"IncedoCRMDesignSystem_7f4257","components":[],"sourceHashes":{"ui_kits/crm-web/account_Form.jsx":"53b19b3a7842","ui_kits/crm-web/admin_extras.jsx":"6ebb087352c1","ui_kits/crm-web/billing_extras.jsx":"65fae6c3d758","ui_kits/crm-web/billing_flows.jsx":"d661a5ca4d17","ui_kits/crm-web/comms.jsx":"086488e34a54","ui_kits/crm-web/complaints.jsx":"4d765b340cf7","ui_kits/crm-web/contacts_activities.jsx":"a53b915230f0","ui_kits/crm-web/crm_companies.jsx":"6fd72dde3400","ui_kits/crm-web/crm_deals.jsx":"d7762ae6cf35","ui_kits/crm-web/portal_Extra.jsx":"443c8e057f72","ui_kits/crm-web/portal_Screens.jsx":"4140267efb2c","ui_kits/crm-web/sub_Data.jsx":"e0ded831e365","ui_kits/crm-web/sub_Primitives.jsx":"99d623d7bc06","ui_kits/crm-web/sub_Screens1.jsx":"ac5a92a6efb6","ui_kits/crm-web/sub_Screens2.jsx":"2452ef730930","ui_kits/crm-web/sub_Screens3.jsx":"d9a4f30343d7","ui_kits/crm-web/sub_Shell.jsx":"bda09c9d0b72","ui_kits/crm-web/template_designer.jsx":"110f5c42a95b"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.IncedoCRMDesignSystem_7f4257 = window.IncedoCRMDesignSystem_7f4257 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// ui_kits/crm-web/account_Form.jsx
try { (() => {
// Account details — shared across portal (self-edit) and admin (customer edit)
// v2: multi-value emails, phones, addresses (with types + primary designation)

const EMAIL_TYPES = ["Work", "Personal", "Other"];
const PHONE_TYPES = ["Mobile", "Work", "Home", "Other"];
const ADDRESS_TYPES = ["Billing", "Contact", "Other"];

// Seed profile — now with arrays instead of scalar fields
const MY_PROFILE = {
  firstName: "Elin",
  lastName: "Karlsson",
  title: "Head of RevOps",
  language: "English (UK)",
  timezone: "Europe/Amsterdam (CET)",
  emails: [{
    id: "em1",
    label: "Work",
    value: "elin.karlsson@orbitlabs.io",
    primary: true
  }, {
    id: "em2",
    label: "Personal",
    value: "elin.k@hey.com",
    primary: false
  }],
  phones: [{
    id: "ph1",
    label: "Mobile",
    value: "+31 6 4412 8807",
    primary: true
  }, {
    id: "ph2",
    label: "Work",
    value: "+31 20 358 1100 ext. 412",
    primary: false
  }],
  addresses: [{
    id: "ad1",
    label: "Billing",
    primary: true,
    line1: "Keizersgracht 318-3",
    line2: "",
    city: "Amsterdam",
    zip: "1016 EZ",
    country: "Netherlands"
  }, {
    id: "ad2",
    label: "Contact",
    primary: true,
    line1: "Haarlemmerplein 17B",
    line2: "3rd floor",
    city: "Amsterdam",
    zip: "1013 HP",
    country: "Netherlands"
  }],
  // Company / tax
  companyName: "Orbit Labs B.V.",
  companyVat: "NL 8032.41.129 B01",
  companyChamber: "KvK 34215982",
  // Prefs
  preferredDeliveryAddressId: "ad2",
  // points at a row in addresses[]
  notifyInvoices: true,
  notifyDunning: true,
  notifyProduct: true,
  notifyMarketing: false,
  twoFactor: true
};
const ADMIN_CUSTOMER_PROFILE = JSON.parse(JSON.stringify(MY_PROFILE));
ADMIN_CUSTOMER_PROFILE.title = "Head of RevOps · Primary contact";

// ── Utilities ──────────────────────────────────────────────────────────
const uid = (p = "id") => `${p}_${Math.random().toString(36).slice(2, 9)}`;

// ── Small building blocks ──────────────────────────────────────────────
function FormSection({
  title,
  subtitle,
  children,
  action
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-form-section"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, subtitle), action && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, action)), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, children));
}
function FormGrid({
  cols = 2,
  children
}) {
  const cls = cols === 2 ? "sp-grid-2" : cols === 3 ? "sp-grid-3" : "sp-grid-2";
  return /*#__PURE__*/React.createElement("div", {
    className: cls
  }, children);
}
function TogglePref({
  value,
  onChange,
  title,
  sub
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 14,
      padding: "12px 0",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, title), sub && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, sub)), /*#__PURE__*/React.createElement("span", {
    onClick: () => onChange(!value),
    style: {
      width: 36,
      height: 20,
      borderRadius: 999,
      cursor: "pointer",
      flexShrink: 0,
      background: value ? "var(--sp-accent-mint)" : "var(--sp-border)",
      position: "relative",
      transition: "background 120ms"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: 2,
      left: value ? 18 : 2,
      width: 16,
      height: 16,
      borderRadius: "50%",
      background: "#fff",
      boxShadow: "0 1px 3px rgba(0,0,0,0.2)",
      transition: "left 160ms cubic-bezier(.4,0,.2,1)"
    }
  })));
}

// Small select that looks like the PInput (same tokens)
function TypeSelect({
  value,
  onChange,
  options
}) {
  return /*#__PURE__*/React.createElement("select", {
    value: value,
    onChange: e => onChange(e.target.value),
    style: {
      height: 36,
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      padding: "0 10px",
      font: "500 13px/18px Roboto",
      outline: "none",
      appearance: "none",
      cursor: "pointer",
      backgroundImage: "linear-gradient(45deg, transparent 50%, var(--sp-text-muted) 50%), linear-gradient(-45deg, transparent 50%, var(--sp-text-muted) 50%)",
      backgroundPosition: "calc(100% - 12px) 50%, calc(100% - 7px) 50%",
      backgroundSize: "5px 5px, 5px 5px",
      backgroundRepeat: "no-repeat",
      paddingRight: 26
    }
  }, options.map(o => /*#__PURE__*/React.createElement("option", {
    key: o,
    value: o
  }, o)));
}

// ── ContactList — shared for emails + phones ───────────────────────────
// Only ONE primary per kind (not per-type), matching what most CRMs do.
function ContactList({
  items,
  onChange,
  kind /* 'email' | 'phone' */,
  typeOptions
}) {
  const placeholder = kind === "email" ? "name@example.com" : "+31 6 0000 0000";
  const leadIcon = kind === "email" ? "✉" : "☎";
  const labelNew = kind === "email" ? "Add email" : "Add phone";
  const update = (id, patch) => onChange(items.map(it => it.id === id ? {
    ...it,
    ...patch
  } : it));
  const remove = id => {
    const filtered = items.filter(it => it.id !== id);
    // If we removed the primary, promote the first remaining item
    if (filtered.length > 0 && !filtered.some(it => it.primary)) filtered[0].primary = true;
    onChange(filtered);
  };
  const setPrimary = id => onChange(items.map(it => ({
    ...it,
    primary: it.id === id
  })));
  const add = () => onChange([...items, {
    id: uid(kind),
    label: typeOptions[0],
    value: "",
    primary: items.length === 0
  }]);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, items.map(it => /*#__PURE__*/React.createElement("div", {
    key: it.id,
    className: "sp-contact-row"
  }, /*#__PURE__*/React.createElement(TypeSelect, {
    value: it.label,
    onChange: v => update(it.id, {
      label: v
    }),
    options: typeOptions
  }), /*#__PURE__*/React.createElement(PInput, {
    value: it.value,
    onChange: v => update(it.id, {
      value: v
    }),
    placeholder: placeholder,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: leadIcon
    })
  }), it.primary ? /*#__PURE__*/React.createElement(PillLabel, {
    tone: "info"
  }, "\u2605 Primary") : /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: () => setPrimary(it.id)
  }, "Make primary"), /*#__PURE__*/React.createElement("span", {
    onClick: () => remove(it.id),
    title: "Remove",
    style: {
      cursor: "pointer",
      padding: 8,
      borderRadius: 8,
      color: "var(--sp-text-muted)",
      font: "400 16px/1 Roboto",
      opacity: items.length === 1 ? 0.3 : 1,
      pointerEvents: items.length === 1 ? "none" : "auto"
    }
  }, "\u2715"))), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    }),
    onClick: add,
    style: {
      alignSelf: "flex-start",
      marginTop: 4
    }
  }, labelNew));
}

// ── AddressList — richer editor, 1 primary PER TYPE (billing has 1 primary, contact has 1 primary) ──
function AddressList({
  items,
  onChange
}) {
  const update = (id, patch) => onChange(items.map(it => it.id === id ? {
    ...it,
    ...patch
  } : it));
  const remove = id => {
    const removed = items.find(it => it.id === id);
    const filtered = items.filter(it => it.id !== id);
    // If we just removed a primary, promote the first remaining address of same type (if any)
    if (removed?.primary) {
      const idx = filtered.findIndex(it => it.label === removed.label);
      if (idx >= 0) filtered[idx] = {
        ...filtered[idx],
        primary: true
      };
    }
    onChange(filtered);
  };
  const setPrimary = id => {
    const target = items.find(it => it.id === id);
    if (!target) return;
    onChange(items.map(it => {
      if (it.label !== target.label) return it;
      return {
        ...it,
        primary: it.id === id
      };
    }));
  };
  const setLabel = (id, newLabel) => {
    // Changing type: if this one was primary, keep it primary in its new type; check conflicts
    const target = items.find(it => it.id === id);
    if (!target) return;
    const conflict = items.find(it => it.id !== id && it.label === newLabel && it.primary);
    onChange(items.map(it => {
      if (it.id !== id) return it;
      return {
        ...it,
        label: newLabel,
        primary: conflict ? false : it.primary
      };
    }));
  };
  const add = (asType = "Contact") => {
    const existingOfType = items.some(it => it.label === asType);
    onChange([...items, {
      id: uid("ad"),
      label: asType,
      primary: !existingOfType,
      // first of its type is primary
      line1: "",
      line2: "",
      city: "",
      zip: "",
      country: "Netherlands"
    }]);
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, items.map(addr => /*#__PURE__*/React.createElement(PCard, {
    key: addr.id,
    style: {
      padding: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(TypeSelect, {
    value: addr.label,
    onChange: v => setLabel(addr.id, v),
    options: ADDRESS_TYPES
  }), addr.primary ? /*#__PURE__*/React.createElement(PillLabel, {
    tone: "info"
  }, "\u2605 Primary ", addr.label.toLowerCase(), " address") : /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: () => setPrimary(addr.id)
  }, "Make primary ", addr.label.toLowerCase()), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    onClick: () => remove(addr.id),
    title: "Remove",
    style: {
      cursor: "pointer",
      padding: 6,
      borderRadius: 6,
      color: "var(--sp-text-muted)",
      font: "400 16px/1 Roboto",
      opacity: items.length === 1 ? 0.3 : 1,
      pointerEvents: items.length === 1 ? "none" : "auto"
    }
  }, "\u2715")), /*#__PURE__*/React.createElement(PInput, {
    label: "Address line 1",
    value: addr.line1,
    onChange: v => update(addr.id, {
      line1: v
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Address line 2 (optional)",
    value: addr.line2,
    onChange: v => update(addr.id, {
      line2: v
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(FormGrid, {
    cols: 3
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "Postal code",
    value: addr.zip,
    onChange: v => update(addr.id, {
      zip: v
    })
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "City",
    value: addr.city,
    onChange: v => update(addr.id, {
      city: v
    })
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Country",
    value: addr.country,
    onChange: v => update(addr.id, {
      country: v
    })
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    }),
    onClick: () => add("Billing")
  }, "Add billing address"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    }),
    onClick: () => add("Contact")
  }, "Add contact address")));
}

// ── DeliveryAddressPicker — dropdown of existing addresses ──────────────
function DeliveryAddressPicker({
  addresses,
  value,
  onChange
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, addresses.map(a => {
    const checked = a.id === value;
    return /*#__PURE__*/React.createElement("label", {
      key: a.id,
      onClick: () => onChange(a.id),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 12,
        padding: "12px 14px",
        borderRadius: 10,
        cursor: "pointer",
        border: `1.5px solid ${checked ? "#1A73E8" : "var(--sp-border)"}`,
        background: checked ? "rgba(26,115,232,.06)" : "var(--sp-surface)"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 16,
        height: 16,
        borderRadius: "50%",
        flexShrink: 0,
        border: `2px solid ${checked ? "#1A73E8" : "var(--sp-text-subtle)"}`,
        background: checked ? "#1A73E8" : "transparent",
        boxShadow: checked ? "inset 0 0 0 3px var(--sp-surface)" : "none"
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, a.label, " address"), a.primary && /*#__PURE__*/React.createElement(PillLabel, {
      tone: "muted"
    }, "Primary")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, a.line1, a.line2 ? `, ${a.line2}` : "", " \xB7 ", a.zip, " ", a.city, ", ", a.country)));
  }));
}

// ── The form itself ────────────────────────────────────────────────────
function AccountForm({
  initial,
  mode,
  onSaved
}) {
  const [p, setP] = React.useState(initial);
  const [saved, setSaved] = React.useState(null);

  // Deep equality for arrays of objects
  const dirty = React.useMemo(() => JSON.stringify(initial) !== JSON.stringify(p), [initial, p]);
  const set = (k, v) => setP(prev => ({
    ...prev,
    [k]: v
  }));
  const discard = () => setP(initial);
  const save = () => {
    setSaved(new Date());
    onSaved && onSaved(p);
  };
  const isAdmin = mode === "admin";
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginBottom: 4
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 72,
      height: 72,
      borderRadius: "50%",
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff",
      font: "600 26px/72px Roboto",
      textAlign: "center"
    }
  }, (p.firstName[0] || "") + (p.lastName[0] || "")), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 20px/26px Roboto",
      color: "var(--sp-text)"
    }
  }, p.firstName, " ", p.lastName), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, p.title, isAdmin ? ` · ${p.companyName}` : ""), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A5"
    })
  }, "Upload photo"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Remove"))), isAdmin && /*#__PURE__*/React.createElement(PillLabel, {
    tone: "plum"
  }, "Editing on behalf of ", p.companyName))), /*#__PURE__*/React.createElement(FormSection, {
    title: "Identity",
    subtitle: isAdmin ? "Primary contact name and role for this customer." : "How your name appears on invoices and in-app."
  }, /*#__PURE__*/React.createElement(FormGrid, {
    cols: 2
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "First name",
    value: p.firstName,
    onChange: v => set("firstName", v)
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Last name",
    value: p.lastName,
    onChange: v => set("lastName", v)
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Job title",
    value: p.title,
    onChange: v => set("title", v)
  })), /*#__PURE__*/React.createElement(FormSection, {
    title: "Email addresses",
    subtitle: "Add as many as you like. The primary is used for invoices and sign-in."
  }, /*#__PURE__*/React.createElement(ContactList, {
    items: p.emails,
    onChange: v => set("emails", v),
    kind: "email",
    typeOptions: EMAIL_TYPES
  })), /*#__PURE__*/React.createElement(FormSection, {
    title: "Phone numbers",
    subtitle: "Used for 2-factor codes and account recovery (primary only)."
  }, /*#__PURE__*/React.createElement(ContactList, {
    items: p.phones,
    onChange: v => set("phones", v),
    kind: "phone",
    typeOptions: PHONE_TYPES
  })), /*#__PURE__*/React.createElement(FormSection, {
    title: "Locale",
    subtitle: "Controls formatting and the language of emails we send."
  }, /*#__PURE__*/React.createElement(FormGrid, {
    cols: 2
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "Language",
    value: p.language,
    onChange: v => set("language", v)
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Time zone",
    value: p.timezone,
    onChange: v => set("timezone", v)
  }))), /*#__PURE__*/React.createElement(FormSection, {
    title: "Company & tax",
    subtitle: isAdmin ? "Legal entity the subscription is billed to." : "Edits here update the details printed on every invoice."
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "Company name",
    value: p.companyName,
    onChange: v => set("companyName", v)
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(FormGrid, {
    cols: 2
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "VAT number",
    value: p.companyVat,
    onChange: v => set("companyVat", v)
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Chamber of Commerce",
    value: p.companyChamber,
    onChange: v => set("companyChamber", v)
  }))), /*#__PURE__*/React.createElement(FormSection, {
    title: "Addresses",
    subtitle: "Billing address is printed on every invoice. Contact address is where we send physical mail."
  }, /*#__PURE__*/React.createElement(AddressList, {
    items: p.addresses,
    onChange: v => set("addresses", v)
  })), /*#__PURE__*/React.createElement(FormSection, {
    title: "Notification preferences",
    subtitle: isAdmin ? "Choose what this contact receives by email." : "Choose what we email you about."
  }, /*#__PURE__*/React.createElement(TogglePref, {
    value: p.notifyInvoices,
    onChange: v => set("notifyInvoices", v),
    title: "Invoices & receipts",
    sub: "Always on for billing contacts \u2014 required by law."
  }), /*#__PURE__*/React.createElement(TogglePref, {
    value: p.notifyDunning,
    onChange: v => set("notifyDunning", v),
    title: "Payment failures & dunning",
    sub: "Notify immediately when a charge fails."
  }), /*#__PURE__*/React.createElement(TogglePref, {
    value: p.notifyProduct,
    onChange: v => set("notifyProduct", v),
    title: "Product updates",
    sub: "New features, changelog, and improvements."
  }), /*#__PURE__*/React.createElement(TogglePref, {
    value: p.notifyMarketing,
    onChange: v => set("notifyMarketing", v),
    title: "Marketing emails",
    sub: "Events, case studies, webinars."
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 18,
      paddingTop: 16,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 4
    }
  }, "Preferred delivery address"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 10
    }
  }, "Default for physical shipments when a product is delivered (hardware, swag, printed contracts). Each product can still override this at the product level."), /*#__PURE__*/React.createElement(DeliveryAddressPicker, {
    addresses: p.addresses,
    value: p.preferredDeliveryAddressId,
    onChange: v => set("preferredDeliveryAddressId", v)
  }))), !isAdmin && /*#__PURE__*/React.createElement(FormSection, {
    title: "Security",
    subtitle: "Protect your account with additional sign-in requirements."
  }, /*#__PURE__*/React.createElement(TogglePref, {
    value: p.twoFactor,
    onChange: v => set("twoFactor", v),
    title: "Two-factor authentication",
    sub: "Require a code from your authenticator app at sign-in."
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginTop: 14
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Change password"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "View active sessions"))), !isAdmin && /*#__PURE__*/React.createElement(FormSection, {
    title: "Danger zone",
    subtitle: "Irreversible and destructive actions."
  }, /*#__PURE__*/React.createElement(PCard, {
    style: {
      borderColor: "rgba(217,48,37,.3)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Delete my account"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Removes personal data. Subscription data retained per the terms of service.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      color: "#D93025"
    }
  }, "Delete account")))), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      bottom: 0,
      marginTop: 20,
      padding: "14px 20px",
      borderRadius: 12,
      background: dirty ? "var(--sp-text)" : "var(--sp-surface)",
      border: dirty ? "none" : "1px solid var(--sp-border)",
      boxShadow: dirty ? "var(--sp-shadow-float)" : "var(--sp-shadow-sm)",
      display: "flex",
      alignItems: "center",
      gap: 12,
      transition: "all 160ms"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      font: "500 13px/18px Roboto",
      color: dirty ? "#fff" : "var(--sp-text-muted)"
    }
  }, dirty ? "You have unsaved changes." : saved ? `Saved · ${saved.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit"
  })}` : "No changes."), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: discard,
    disabled: !dirty,
    style: {
      color: dirty ? "#fff" : "var(--sp-text-muted)"
    }
  }, "Discard"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    onClick: save,
    disabled: !dirty
  }, "Save changes")));
}

// ── Wrappers ───────────────────────────────────────────────────────────
function PAccount({
  onSaved
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "40px 48px",
      maxWidth: 960,
      margin: "0 auto"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Account settings"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 20
    }
  }, "Update your name, contact details, addresses, and preferences."), /*#__PURE__*/React.createElement(AccountForm, {
    initial: MY_PROFILE,
    mode: "self",
    onSaved: onSaved
  }));
}
function AdminCustomerEdit({
  customerName,
  onBack,
  onSaved
}) {
  const profile = {
    ...ADMIN_CUSTOMER_PROFILE,
    companyName: customerName || ADMIN_CUSTOMER_PROFILE.companyName
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "32px 40px",
      maxWidth: 1100,
      margin: "0 auto"
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 4
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 26px/32px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.01em"
    }
  }, "Customer details"), /*#__PURE__*/React.createElement(PillLabel, {
    tone: "info"
  }, "Primary contact")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 16
    }
  }, "Edit the contact, addresses, and notification preferences for ", /*#__PURE__*/React.createElement("b", {
    style: {
      color: "var(--sp-text)",
      fontWeight: 500
    }
  }, profile.companyName), "."), /*#__PURE__*/React.createElement(AccountForm, {
    initial: profile,
    mode: "admin",
    onSaved: onSaved
  }));
}
Object.assign(window, {
  MY_PROFILE,
  ADMIN_CUSTOMER_PROFILE,
  EMAIL_TYPES,
  PHONE_TYPES,
  ADDRESS_TYPES,
  AccountForm,
  PAccount,
  AdminCustomerEdit,
  FormSection,
  FormGrid,
  TogglePref,
  TypeSelect,
  ContactList,
  AddressList,
  DeliveryAddressPicker
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/account_Form.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/admin_extras.jsx
try { (() => {
// ─────────────────────────────────────────────────────────────────────────
// Admin extras — Customer 360 + My Work inbox.
//
// Customer 360: a single page that joins a company/contact with their
// subscriptions, invoices, complaints, tickets, and a unified timeline.
// The entry point is usually "open from a complaint/sub/dunning row".
//
// My Work: a unified inbox for a case-handler — complaints + tickets +
// failed-payment follow-ups + renewal touches + overdue activities,
// all sorted by an SLA-derived priority signal.
// ─────────────────────────────────────────────────────────────────────────

// ─── Fixture: a few "deeply-linked" customers for the 360 view ─────────
// Keyed by customer name; merges with SUBS, INVOICES, ADMIN_COMPLAINTS by string match.
const CUSTOMER_PROFILES = {
  "Orbit Labs B.V.": {
    name: "Orbit Labs B.V.",
    shortName: "Orbit Labs",
    logoColor: "#1A73E8",
    tier: "Growth",
    ownerAE: "Bram de Vries",
    ownerCSM: "Priya Shah",
    domain: "orbitlabs.io",
    vatId: "NL 8211.43.667.B01",
    country: "Netherlands",
    industry: "Space tech · Manufacturing",
    size: "120 employees",
    mrr: 21600,
    ltv: 648000,
    since: "Jan 08, 2024",
    nps: 42,
    health: "at-risk",
    healthReason: "Open complaint · billing category",
    primaryContact: {
      name: "Elin Karlsson",
      role: "COO",
      email: "elin@orbitlabs.io",
      phone: "+31 6 2211 4455"
    },
    contacts: [{
      name: "Elin Karlsson",
      role: "COO",
      email: "elin@orbitlabs.io",
      phone: "+31 6 2211 4455",
      primary: true
    }, {
      name: "Fredrik Sund",
      role: "Finance lead",
      email: "fredrik@orbitlabs.io",
      phone: "+31 6 4811 0912"
    }, {
      name: "Matilda Järvinen",
      role: "Admin",
      email: "matilda@orbitlabs.io",
      phone: null
    }],
    tags: ["enterprise-lite", "eu-north", "beta-tester"],
    lifetime: {
      tickets: 34,
      complaints: 2,
      refunds: 21600,
      upgrades: 2,
      pauses: 0
    }
  },
  "Northwind GmbH": {
    name: "Northwind GmbH",
    shortName: "Northwind",
    logoColor: "#E8710A",
    tier: "Enterprise",
    ownerAE: "Anna Krause",
    ownerCSM: "Daan Visser",
    domain: "northwind.de",
    vatId: "DE 288 744 219",
    country: "Germany",
    industry: "Logistics",
    size: "840 employees",
    mrr: 38400,
    ltv: 1344000,
    since: "Nov 03, 2022",
    nps: 28,
    health: "red",
    healthReason: "Payment failed 3× · €38,400 overdue",
    primaryContact: {
      name: "Heinrich Vogel",
      role: "CFO",
      email: "h.vogel@northwind.de",
      phone: "+49 89 4412 7781"
    },
    contacts: [{
      name: "Heinrich Vogel",
      role: "CFO",
      email: "h.vogel@northwind.de",
      phone: "+49 89 4412 7781",
      primary: true
    }, {
      name: "Lena Krause",
      role: "Procurement",
      email: "l.krause@northwind.de",
      phone: null
    }],
    tags: ["enterprise", "dach", "at-risk"],
    lifetime: {
      tickets: 68,
      complaints: 5,
      refunds: 0,
      upgrades: 3,
      pauses: 1
    }
  }
};

// Unified timeline rows — mixed event types with type tags.
const CUSTOMER_TIMELINE = {
  "Orbit Labs B.V.": [{
    t: "complaint",
    when: "2h ago",
    title: "Complaint opened — Charged twice for October",
    meta: "Priority: High · handled by Priya Shah",
    ref: "C-20260418-0017",
    tone: "warm"
  }, {
    t: "email-out",
    when: "1h 20m ago",
    title: "Reply sent to Elin Karlsson",
    meta: "Subject: Re: Duplicate invoice — refund initiated",
    tone: "info"
  }, {
    t: "invoice",
    when: "Apr 18",
    title: "Invoice INV-20251002 · €216.00",
    meta: "Paid · duplicate of INV-20251001",
    tone: "muted"
  }, {
    t: "email-in",
    when: "Apr 18",
    title: "Email from Elin Karlsson",
    meta: "\"I'm noticing I've been charged twice for October. Can someone take a look?\"",
    tone: "muted"
  }, {
    t: "upgrade",
    when: "Mar 01",
    title: "Upgraded from Starter → Growth",
    meta: "ARR +€180,000 · owned by Bram de Vries",
    tone: "mint"
  }, {
    t: "complaint",
    when: "Apr 02",
    title: "Complaint closed — Cancellation flow confusing",
    meta: "Root cause: journey.cancellation.friction · CSAT 4/5",
    ref: "C-20260402-0008",
    tone: "muted"
  }, {
    t: "call",
    when: "Apr 02",
    title: "Inbound call · 11m",
    meta: "Daan Visser · de-escalation, explained cancellation steps",
    tone: "muted"
  }, {
    t: "ticket",
    when: "Mar 22",
    title: "Support ticket — SSO misconfig",
    meta: "Resolved in 1h 20m · Medium",
    tone: "muted"
  }, {
    t: "note",
    when: "Jan 08",
    title: "Customer created",
    meta: "Signed up via sales-assisted · owner Bram",
    tone: "muted"
  }],
  "Northwind GmbH": [{
    t: "dunning",
    when: "Today",
    title: "Dunning step 3 — final notice",
    meta: "Invoice INV-20251003 · €384.00 · 10 days overdue",
    tone: "warm"
  }, {
    t: "payment",
    when: "Today",
    title: "Payment retry failed",
    meta: "Stripe: insufficient_funds · •• 7714",
    tone: "warm"
  }, {
    t: "email-out",
    when: "Yesterday",
    title: "Automated dunning email sent",
    meta: "Delivered · opened 2× · no reply",
    tone: "info"
  }, {
    t: "complaint",
    when: "Apr 16",
    title: "Complaint opened — DSR export not received",
    meta: "Priority: Urgent · SLA breached · handled by Priya Shah",
    ref: "C-20260416-0004",
    tone: "warm"
  }, {
    t: "invoice",
    when: "Apr 01",
    title: "Invoice INV-20251003 · €384.00",
    meta: "Overdue · 3 attempts",
    tone: "muted"
  }, {
    t: "upgrade",
    when: "Aug 2024",
    title: "Seat expansion 60 → 85",
    meta: "ARR +€72,000",
    tone: "mint"
  }]
};

// ─── Cross-module lookups ───────────────────────────────────────────────
function _nameMatches(a, b) {
  if (!a || !b) return false;
  if (a === b) return true;
  const strip = s => s.replace(/\s+(B\.V\.|GmbH|NV|Ltd|LLP|Holdings)$/i, "").trim().toLowerCase();
  return strip(a) === strip(b);
}
function findSubsByCustomer(name) {
  return (typeof SUBS !== "undefined" ? SUBS : []).filter(s => _nameMatches(s.customer, name));
}
function findInvoicesByCustomer(name) {
  return (typeof INVOICES !== "undefined" ? INVOICES : []).filter(i => _nameMatches(i.customer, name));
}
function findComplaintsByCustomer(name) {
  return (typeof ADMIN_COMPLAINTS !== "undefined" ? ADMIN_COMPLAINTS : []).filter(c => c.customer === name);
}

// ─── Shared pieces ──────────────────────────────────────────────────────
function CustomerAvatar({
  profile,
  size = 52
}) {
  const name = profile.shortName || profile.name || "—";
  const initials = name.split(/\s+/).slice(0, 2).map(s => s[0] || "").join("").toUpperCase() || "?";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: size,
      height: size,
      borderRadius: 10,
      background: "color-mix(in srgb, " + profile.logoColor + " 16%, transparent)",
      color: profile.logoColor,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      font: "700 " + Math.round(size * 0.4) + "px/1 Roboto",
      letterSpacing: "-0.02em",
      flex: "none"
    }
  }, initials);
}
function HealthDot({
  health
}) {
  const color = health === "green" ? "var(--sp-accent-mint)" : health === "at-risk" ? "#F9A825" : health === "red" ? "#D93025" : "var(--sp-text-muted)";
  const label = health === "green" ? "Healthy" : health === "at-risk" ? "At risk" : health === "red" ? "Critical" : "Unknown";
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      font: "500 12px/16px Roboto",
      color
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: color,
      boxShadow: `0 0 0 3px color-mix(in srgb, ${color} 20%, transparent)`
    }
  }), label);
}
function TimelineRow({
  ev
}) {
  const glyphs = {
    "complaint": "◉",
    "email-in": "↙",
    "email-out": "↗",
    "call": "☏",
    "ticket": "✉",
    "invoice": "€",
    "payment": "▣",
    "dunning": "⚠",
    "upgrade": "↑",
    "note": "◌"
  };
  const tones = {
    warm: {
      bg: "color-mix(in srgb, #D93025 8%, transparent)",
      fg: "#D93025"
    },
    info: {
      bg: "color-mix(in srgb, #1A73E8 8%, transparent)",
      fg: "#1A73E8"
    },
    mint: {
      bg: "color-mix(in srgb, var(--sp-accent-mint) 12%, transparent)",
      fg: "var(--sp-accent-mint)"
    },
    muted: {
      bg: "var(--sp-surface-2)",
      fg: "var(--sp-text-muted)"
    }
  };
  const t = tones[ev.tone] || tones.muted;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 96,
      flex: "none",
      font: "500 11px/16px Roboto",
      color: "var(--sp-text-muted)",
      textAlign: "right",
      paddingTop: 8
    }
  }, ev.when), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 28,
      height: 28,
      borderRadius: 7,
      background: t.bg,
      color: t.fg,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      font: "500 14px/1 Roboto",
      flex: "none",
      marginTop: 4
    }
  }, glyphs[ev.t] || "•"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      paddingBottom: 16,
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, ev.title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, ev.meta), ev.ref && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6,
      font: "500 11px/14px 'Roboto Mono', monospace",
      color: "var(--sp-text-subtle)"
    }
  }, ev.ref)));
}

// ────────────────────────────────────────────────────────────────────────
// CUSTOMER 360
// ────────────────────────────────────────────────────────────────────────
function Customer360({
  customerName,
  onBack,
  onOpenComplaint,
  onOpenInvoice,
  onOpenSub
}) {
  if (!customerName) {
    return /*#__PURE__*/React.createElement("div", {
      className: "sp-page"
    }, /*#__PURE__*/React.createElement("span", {
      onClick: onBack,
      style: {
        cursor: "pointer",
        font: "500 13px/18px Roboto",
        color: "var(--sp-text-muted)",
        marginBottom: 12,
        display: "inline-block"
      }
    }, "\u2190 Back"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 14px/20px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "No customer selected.")));
  }
  // Fall back to a stubbed profile if not in our rich set
  const profile = CUSTOMER_PROFILES[customerName] || {
    name: customerName,
    shortName: customerName,
    logoColor: "#5F6368",
    tier: "—",
    ownerAE: "—",
    ownerCSM: "—",
    domain: "—",
    vatId: "—",
    country: "—",
    industry: "—",
    size: "—",
    mrr: 0,
    ltv: 0,
    since: "—",
    nps: null,
    health: "green",
    healthReason: "",
    primaryContact: {
      name: "—",
      role: "—",
      email: "—",
      phone: "—"
    },
    contacts: [],
    tags: [],
    lifetime: {
      tickets: 0,
      complaints: 0,
      refunds: 0,
      upgrades: 0,
      pauses: 0
    }
  };
  const subs = findSubsByCustomer(profile.name);
  const invoices = findInvoicesByCustomer(profile.name);
  const complaints = findComplaintsByCustomer(profile.name);
  const timeline = CUSTOMER_TIMELINE[profile.name] || [];
  const [tab, setTab] = React.useState("overview");
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(CustomerAvatar, {
    profile: profile,
    size: 64
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 280
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, profile.name), /*#__PURE__*/React.createElement(HealthDot, {
    health: profile.health
  }), /*#__PURE__*/React.createElement(PillLabel, {
    tone: "info"
  }, profile.tier)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, profile.industry, " \xB7 ", profile.size, " \xB7 ", profile.country, " \xB7 customer since ", profile.since), profile.healthReason && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      display: "inline-flex",
      gap: 8,
      alignItems: "center",
      padding: "6px 10px",
      borderRadius: 6,
      background: profile.health === "red" ? "color-mix(in srgb, #D93025 10%, transparent)" : "color-mix(in srgb, #F9A825 12%, transparent)",
      color: profile.health === "red" ? "#D93025" : "#E8710A",
      font: "500 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement("span", null, profile.health === "red" ? "◉" : "◎"), " ", profile.healthReason), profile.tags.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      flexWrap: "wrap",
      marginTop: 14
    }
  }, profile.tags.map(t => /*#__PURE__*/React.createElement("span", {
    key: t,
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      padding: "3px 8px",
      borderRadius: 4,
      background: "var(--sp-surface-2)",
      border: "1px solid var(--sp-border)"
    }
  }, t)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u270E"
    })
  }, "Edit"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    })
  }, "Email"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u25C9"
    })
  }, "Log complaint"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "New note"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(120px, 1fr))",
      gap: 18,
      marginTop: 24,
      paddingTop: 20,
      borderTop: "1px solid var(--sp-border)"
    }
  }, [{
    label: "MRR",
    value: fmtMoney(profile.mrr * 100)
  }, {
    label: "Lifetime value",
    value: fmtMoney(profile.ltv * 100)
  }, {
    label: "Subscriptions",
    value: subs.length
  }, {
    label: "Open invoices",
    value: invoices.filter(i => i.status === "pending" || i.status === "overdue").length
  }, {
    label: "Open complaints",
    value: complaints.filter(c => c.status !== "CLOSED" && c.status !== "REJECTED").length
  }, {
    label: "NPS",
    value: profile.nps !== null ? profile.nps : "—"
  }].map(k => /*#__PURE__*/React.createElement("div", {
    key: k.label
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, k.label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 22px/28px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, k.value))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 18
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    value: tab,
    onChange: setTab,
    options: [{
      value: "overview",
      label: "Overview"
    }, {
      value: "subs",
      label: `Subscriptions · ${subs.length}`
    }, {
      value: "billing",
      label: `Billing · ${invoices.length}`
    }, {
      value: "complaints",
      label: `Complaints · ${complaints.length}`
    }, {
      value: "contacts",
      label: `Contacts · ${profile.contacts.length}`
    }, {
      value: "activity",
      label: "Activity"
    }]
  })), tab === "overview" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 340px",
      gap: 20,
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Active subscriptions"), subs.length > 0 && /*#__PURE__*/React.createElement("span", {
    onClick: () => setTab("subs"),
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "#1A73E8"
    }
  }, "See all \u2192")), subs.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "No subscriptions on file.") : subs.slice(0, 3).map(s => /*#__PURE__*/React.createElement("div", {
    key: s.id,
    onClick: () => onOpenSub && onOpenSub(s),
    style: {
      display: "grid",
      gridTemplateColumns: "120px 1fr 120px 80px",
      gap: 12,
      alignItems: "center",
      padding: "10px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, s.id), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, s.plan), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, s.seats, " seats \xB7 renews ", s.next)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(s.mrr * 100), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "/mo")), /*#__PURE__*/React.createElement(PillLabel, {
    tone: s.status === "active" ? "mint" : s.status === "past_due" ? "warm" : "muted"
  }, s.status)))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Recent invoices"), invoices.length > 0 && /*#__PURE__*/React.createElement("span", {
    onClick: () => setTab("billing"),
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "#1A73E8"
    }
  }, "See all \u2192")), invoices.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "No invoices yet.") : invoices.slice(0, 4).map(i => /*#__PURE__*/React.createElement("div", {
    key: i.id,
    onClick: () => onOpenInvoice && onOpenInvoice(i),
    style: {
      display: "grid",
      gridTemplateColumns: "140px 1fr 100px 100px",
      gap: 12,
      alignItems: "center",
      padding: "8px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, i.id), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Due ", i.due), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      textAlign: "right"
    }
  }, fmtMoney(i.amount * 100)), /*#__PURE__*/React.createElement(PillLabel, {
    tone: i.status === "paid" ? "mint" : i.status === "overdue" ? "warm" : i.status === "pending" ? "amber" : "muted"
  }, i.status)))), complaints.length > 0 && /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Complaints"), /*#__PURE__*/React.createElement("span", {
    onClick: () => setTab("complaints"),
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "#1A73E8"
    }
  }, "See all \u2192")), complaints.slice(0, 3).map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    onClick: () => onOpenComplaint && onOpenComplaint(c),
    style: {
      display: "flex",
      gap: 12,
      alignItems: "flex-start",
      padding: "10px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, c.id), /*#__PURE__*/React.createElement(CxStatusChip, {
    status: c.status
  }), /*#__PURE__*/React.createElement(CxPriorityPill, {
    priority: c.priority
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, c.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Handler: ", c.assignee, " \xB7 filed ", c.received)))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Primary contact"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 8
    }
  }, profile.primaryContact.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, profile.primaryContact.role), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "#1A73E8",
      marginTop: 8
    }
  }, profile.primaryContact.email), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, profile.primaryContact.phone)), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Account team"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Account executive"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, profile.ownerAE)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Customer success"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, profile.ownerCSM)))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Company details"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(KV, {
    k: "Domain",
    v: profile.domain
  }), /*#__PURE__*/React.createElement(KV, {
    k: "VAT ID",
    v: profile.vatId
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Country",
    v: profile.country
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Industry",
    v: profile.industry
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Headcount",
    v: profile.size
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Lifetime"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 10,
      marginTop: 10
    }
  }, [{
    k: "Tickets",
    v: profile.lifetime.tickets
  }, {
    k: "Complaints",
    v: profile.lifetime.complaints
  }, {
    k: "Refunds",
    v: fmtMoney(profile.lifetime.refunds * 100)
  }, {
    k: "Upgrades",
    v: profile.lifetime.upgrades
  }, {
    k: "Pauses",
    v: profile.lifetime.pauses
  }].map(x => /*#__PURE__*/React.createElement("div", {
    key: x.k
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, x.k), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, x.v))))))), tab === "subs" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "All subscriptions (", subs.length, ")"), subs.map(s => /*#__PURE__*/React.createElement("div", {
    key: s.id,
    onClick: () => onOpenSub && onOpenSub(s),
    style: {
      display: "grid",
      gridTemplateColumns: "140px 1fr 120px 120px 100px",
      gap: 12,
      alignItems: "center",
      padding: "12px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, s.id), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, s.plan), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, s.seats, " seats \xB7 since ", s.since)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(s.mrr * 100), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "/mo")), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Renews ", s.next), /*#__PURE__*/React.createElement(PillLabel, {
    tone: s.status === "active" ? "mint" : s.status === "past_due" ? "warm" : "muted"
  }, s.status)))), tab === "billing" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "All invoices (", invoices.length, ")"), invoices.map(i => /*#__PURE__*/React.createElement("div", {
    key: i.id,
    onClick: () => onOpenInvoice && onOpenInvoice(i),
    style: {
      display: "grid",
      gridTemplateColumns: "160px 120px 120px 1fr 100px",
      gap: 12,
      alignItems: "center",
      padding: "12px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, i.id), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Issued ", i.issued), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Due ", i.due), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(i.amount * 100)), /*#__PURE__*/React.createElement(PillLabel, {
    tone: i.status === "paid" ? "mint" : i.status === "overdue" ? "warm" : i.status === "pending" ? "amber" : "muted"
  }, i.status)))), tab === "complaints" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "All complaints (", complaints.length, ")"), complaints.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "No complaints filed. \u2713") : complaints.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    onClick: () => onOpenComplaint && onOpenComplaint(c),
    style: {
      padding: "14px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, c.id), /*#__PURE__*/React.createElement(CxStatusChip, {
    status: c.status
  }), /*#__PURE__*/React.createElement(CxPriorityPill, {
    priority: c.priority
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 15px/22px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, c.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, CX_CATEGORIES.find(x => x.k === c.category)?.label, " \xB7 handler ", c.assignee, " \xB7 filed ", c.received)))), tab === "contacts" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Contacts (", profile.contacts.length, ")"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Add contact")), profile.contacts.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.email,
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1fr 80px",
      gap: 12,
      alignItems: "center",
      padding: "12px 0",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, c.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.role)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "#1A73E8"
    }
  }, c.email), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.phone || "—"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, c.primary && /*#__PURE__*/React.createElement(PillLabel, {
    tone: "info"
  }, "Primary"))))), tab === "activity" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 16
    }
  }, "Unified timeline"), timeline.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "No activity yet.") : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, timeline.map((ev, i) => /*#__PURE__*/React.createElement(TimelineRow, {
    key: i,
    ev: ev
  })))));
}
function KV({
  k,
  v
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, k), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      textAlign: "right"
    }
  }, v));
}

// ────────────────────────────────────────────────────────────────────────
// MY WORK — unified inbox for a case-handler
// ────────────────────────────────────────────────────────────────────────
// Merges: open complaints assigned to me + failing-payment dunning rows +
// overdue activities + open support tickets + upcoming renewals.

const MY_ACTIVITIES = [{
  id: "act-301",
  t: "call",
  title: "Follow-up call · Hanzeborg NV",
  meta: "Re: Q4 seat expansion",
  due: "Today 14:00",
  customer: "Hanzeborg NV",
  owner: "Priya Shah",
  overdue: false
}, {
  id: "act-288",
  t: "task",
  title: "Draft refund memo · Orbit Labs",
  meta: "For C-20260418-0017",
  due: "Today 17:00",
  customer: "Orbit Labs B.V.",
  owner: "Priya Shah",
  overdue: false
}, {
  id: "act-241",
  t: "email",
  title: "Email · Meridian Fintech",
  meta: "Renewal packet",
  due: "Yesterday",
  customer: "Meridian Fintech",
  owner: "Priya Shah",
  overdue: true
}];
const MY_RENEWALS = [{
  id: "sub_002",
  customer: "Orbit Labs B.V.",
  plan: "Growth",
  mrr: 21600,
  next: "Oct 28",
  daysLeft: 4
}, {
  id: "sub_005",
  customer: "Hanzeborg NV",
  plan: "Growth",
  mrr: 16800,
  next: "Nov 02",
  daysLeft: 9
}];
function MyWork({
  onOpenComplaint,
  onOpenInvoice,
  onOpenCustomer,
  onNavigate
}) {
  const [filter, setFilter] = React.useState("all"); // all / complaints / billing / activities / renewals
  const me = "Priya Shah";

  // Build unified, prioritised list
  const items = [];

  // Complaints assigned to me that are open
  (typeof ADMIN_COMPLAINTS !== "undefined" ? ADMIN_COMPLAINTS : []).filter(c => c.assignee === me && c.status !== "CLOSED" && c.status !== "REJECTED").forEach(c => {
    const slaMinutes = c.slaResMinutesLeft;
    const bucket = c.slaResBreach ? "breach" : slaMinutes !== null && slaMinutes < 240 ? "soon" : "normal";
    items.push({
      kind: "complaint",
      id: c.id,
      title: c.subject,
      customer: c.customer,
      meta: `${CX_CATEGORIES.find(x => x.k === c.category)?.label} · ${CX_PRIORITIES.find(p => p.k === c.priority)?.label}`,
      due: c.slaResBreach ? `breached ${Math.abs(Math.round((slaMinutes || 0) / 60))}h ago` : slaMinutes !== null ? `${Math.round(slaMinutes / 60)}h to SLA` : "on track",
      bucket,
      priority: c.priority === "URGENT" ? 4 : c.priority === "HIGH" ? 3 : c.priority === "MEDIUM" ? 2 : 1,
      raw: c
    });
  });

  // Failing-payment dunning I'm on
  (typeof DUNNING !== "undefined" ? DUNNING : []).filter(d => d.severity === "high").slice(0, 3).forEach(d => {
    items.push({
      kind: "dunning",
      id: d.id,
      title: `Payment failed · ${d.customer}`,
      customer: d.customer,
      meta: `${d.reason.replace(/_/g, " ")} · ${d.attempts} attempts · ${fmtMoney(d.amount * 100)}`,
      due: `retry ${d.nextRetry}`,
      bucket: d.age > 7 ? "breach" : "soon",
      priority: d.age > 10 ? 4 : 3,
      raw: d
    });
  });

  // Overdue activities
  MY_ACTIVITIES.forEach(a => {
    items.push({
      kind: "activity",
      id: a.id,
      title: a.title,
      customer: a.customer,
      meta: a.meta,
      due: a.due,
      bucket: a.overdue ? "breach" : "normal",
      priority: a.overdue ? 3 : 1,
      raw: a
    });
  });

  // Upcoming renewals (touchpoint)
  MY_RENEWALS.forEach(r => {
    items.push({
      kind: "renewal",
      id: r.id,
      title: `Renewal touch · ${r.customer}`,
      customer: r.customer,
      meta: `${r.plan} · ${fmtMoney(r.mrr * 100)} MRR · renews ${r.next}`,
      due: `${r.daysLeft}d`,
      bucket: r.daysLeft <= 5 ? "soon" : "normal",
      priority: r.daysLeft <= 5 ? 2 : 1,
      raw: r
    });
  });

  // Sort: breach → soon → normal, then priority desc
  const bucketRank = {
    breach: 0,
    soon: 1,
    normal: 2
  };
  items.sort((a, b) => {
    if (bucketRank[a.bucket] !== bucketRank[b.bucket]) return bucketRank[a.bucket] - bucketRank[b.bucket];
    return b.priority - a.priority;
  });
  const filtered = filter === "all" ? items : filter === "complaints" ? items.filter(x => x.kind === "complaint") : filter === "billing" ? items.filter(x => x.kind === "dunning") : filter === "activities" ? items.filter(x => x.kind === "activity") : filter === "renewals" ? items.filter(x => x.kind === "renewal") : items;
  const counts = {
    all: items.length,
    complaints: items.filter(x => x.kind === "complaint").length,
    billing: items.filter(x => x.kind === "dunning").length,
    activities: items.filter(x => x.kind === "activity").length,
    renewals: items.filter(x => x.kind === "renewal").length,
    breach: items.filter(x => x.bucket === "breach").length,
    soon: items.filter(x => x.bucket === "soon").length
  };
  const kindGlyphs = {
    complaint: "◉",
    dunning: "⚠",
    activity: "✓",
    renewal: "↻",
    ticket: "✉"
  };
  const kindTone = {
    complaint: "warm",
    dunning: "warm",
    activity: "info",
    renewal: "mint"
  };
  const handleOpen = item => {
    if (item.kind === "complaint") onOpenComplaint && onOpenComplaint(item.raw);else if (item.kind === "dunning") onOpenInvoice && onOpenInvoice(item.raw);else if (item.kind === "renewal" || item.kind === "activity") onOpenCustomer && onOpenCustomer(item.customer);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "My work"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Everything on your plate, sorted by urgency. Case-handler view.")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u25CE"
    })
  }, "Out of office"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Log activity"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
      gap: 14,
      marginTop: 24
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "On your plate"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, counts.all)), /*#__PURE__*/React.createElement(PCard, {
    style: {
      background: counts.breach > 0 ? "color-mix(in srgb, #D93025 5%, transparent)" : undefined
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "#D93025",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "SLA breached"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: counts.breach > 0 ? "#D93025" : "var(--sp-text)",
      marginTop: 6
    }
  }, counts.breach)), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "#E8710A",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Due < 4h"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: counts.soon > 0 ? "#E8710A" : "var(--sp-text)",
      marginTop: 6
    }
  }, counts.soon)), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Closed this week"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, "11"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-accent-mint)",
      marginTop: 2
    }
  }, "+2 vs last week"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginTop: 24,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: filter,
    onChange: setFilter,
    options: [{
      value: "all",
      label: `All · ${counts.all}`
    }, {
      value: "complaints",
      label: `Complaints · ${counts.complaints}`
    }, {
      value: "billing",
      label: `Billing · ${counts.billing}`
    }, {
      value: "activities",
      label: `Activities · ${counts.activities}`
    }, {
      value: "renewals",
      label: `Renewals · ${counts.renewals}`
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Sorted by SLA urgency")), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      marginTop: 16,
      overflow: "hidden"
    }
  }, filtered.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "Nothing in this bucket. \uD83C\uDF89") : filtered.map((it, i) => {
    const tone = kindTone[it.kind] || "muted";
    const toneColor = tone === "warm" ? "#D93025" : tone === "info" ? "#1A73E8" : tone === "mint" ? "var(--sp-accent-mint)" : "var(--sp-text-muted)";
    const bucketBorder = it.bucket === "breach" ? "#D93025" : it.bucket === "soon" ? "#E8710A" : "transparent";
    return /*#__PURE__*/React.createElement("div", {
      key: it.kind + "-" + it.id + "-" + i,
      onClick: () => handleOpen(it),
      style: {
        display: "grid",
        gridTemplateColumns: "4px 40px 1fr 180px 140px 28px",
        gap: 12,
        alignItems: "center",
        padding: "14px 16px 14px 0",
        borderBottom: "1px solid var(--sp-border)",
        cursor: "pointer",
        background: "transparent",
        transition: "background 120ms"
      },
      onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
      onMouseLeave: e => e.currentTarget.style.background = "transparent"
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        alignSelf: "stretch",
        background: bucketBorder,
        borderRadius: "0 2px 2px 0"
      }
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        width: 32,
        height: 32,
        borderRadius: 8,
        background: "color-mix(in srgb, " + toneColor + " 12%, transparent)",
        color: toneColor,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        font: "500 14px/1 Roboto",
        marginLeft: 12
      }
    }, kindGlyphs[it.kind]), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8,
        flexWrap: "wrap"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, it.title), it.bucket === "breach" && /*#__PURE__*/React.createElement(PillLabel, {
      tone: "warm"
    }, "Breached"), it.bucket === "soon" && /*#__PURE__*/React.createElement(PillLabel, {
      tone: "amber"
    }, "Urgent")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text)",
        font: "500 12px/16px Roboto"
      }
    }, it.customer), it.meta && /*#__PURE__*/React.createElement(React.Fragment, null, " \xB7 ", it.meta))), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 12px/16px 'Roboto Mono',monospace",
        color: "var(--sp-text-subtle)"
      }
    }, it.id), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 12px/16px Roboto",
        color: it.bucket === "breach" ? "#D93025" : it.bucket === "soon" ? "#E8710A" : "var(--sp-text-muted)",
        textAlign: "right"
      }
    }, it.due), /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-subtle)",
        textAlign: "right",
        paddingRight: 4
      }
    }, "\u203A"));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", filtered.length, " of ", items.length, " items \xB7 you are ", /*#__PURE__*/React.createElement("strong", null, me)));
}

// ────────────────────────────────────────────────────────────────────────
// Export
// ────────────────────────────────────────────────────────────────────────
Object.assign(window, {
  CUSTOMER_PROFILES,
  CUSTOMER_TIMELINE,
  findSubsByCustomer,
  findInvoicesByCustomer,
  findComplaintsByCustomer,
  Customer360,
  MyWork,
  CustomerAvatar,
  HealthDot,
  TimelineRow,
  KV
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/admin_extras.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/billing_extras.jsx
try { (() => {
// Billing & money — extra modules: Pricing catalog, Payments list, SEPA, Ledger, Templates
// Read-mostly screens that round out the billing surface beyond the existing
// Subscriptions / Invoices / Dunning / Plans / Payment-methods pages.

const {
  useState: useStBE,
  useMemo: useMemoBE
} = React;

// ─── Shared bits ────────────────────────────────────────────────────────
const beEur = (cents, opts = {}) => ((cents || 0) / 100).toLocaleString("nl-NL", {
  style: "currency",
  currency: "EUR",
  maximumFractionDigits: opts.dec ?? 2,
  minimumFractionDigits: opts.dec ?? 2
});
function BEPill({
  tone = "info",
  children,
  size = "md"
}) {
  const map = {
    info: {
      bg: "rgba(26,115,232,.10)",
      fg: "#1A73E8"
    },
    mint: {
      bg: "rgba(16,140,107,.10)",
      fg: "#108C6B"
    },
    warm: {
      bg: "rgba(217,48,37,.10)",
      fg: "#D93025"
    },
    plum: {
      bg: "rgba(140,77,168,.10)",
      fg: "#8C4DA8"
    },
    amber: {
      bg: "rgba(196,138,28,.12)",
      fg: "#A2710C"
    },
    muted: {
      bg: "var(--sp-surface-soft, rgba(0,0,0,.06))",
      fg: "var(--sp-text-muted)"
    }
  };
  const c = map[tone] || map.info;
  const sz = size === "sm" ? {
    padding: "1px 7px",
    font: "500 10px/14px Roboto"
  } : {
    padding: "2px 9px",
    font: "500 11px/16px Roboto"
  };
  return /*#__PURE__*/React.createElement("span", {
    style: {
      ...sz,
      background: c.bg,
      color: c.fg,
      borderRadius: 999,
      whiteSpace: "nowrap"
    }
  }, children);
}
function BEHeader({
  title,
  subtitle,
  actions,
  right
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-end",
      justifyContent: "space-between",
      gap: 24,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 26px/32px Roboto",
      color: "var(--sp-text)"
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, subtitle)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      alignItems: "center"
    }
  }, right, actions));
}
function BEKpi({
  label,
  value,
  delta,
  tone
}) {
  return /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 24px/30px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, value), delta && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: tone === "down" ? "#D93025" : tone === "up" ? "#108C6B" : "var(--sp-text-muted)",
      marginTop: 2
    }
  }, delta));
}
function BETabs({
  value,
  onChange,
  options
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: 3
    }
  }, options.map(o => {
    const on = o.value === value;
    return /*#__PURE__*/React.createElement("button", {
      key: o.value,
      onClick: () => onChange(o.value),
      style: {
        border: "none",
        borderRadius: 6,
        padding: "6px 12px",
        font: "500 12px/16px Roboto",
        background: on ? "#1A73E8" : "transparent",
        color: on ? "#fff" : "var(--sp-text-muted)",
        cursor: "pointer"
      }
    }, o.label, o.count != null && /*#__PURE__*/React.createElement("span", {
      style: {
        marginLeft: 6,
        opacity: .8
      }
    }, o.count));
  }));
}
function BERowChev() {
  return /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)",
      fontSize: 16
    }
  }, "\u203A");
}

// Reusable side label (already exists in sub_Screens2/portal — re-declare local)
function BESideLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em"
    }
  }, children);
}

// ════════════════════════════════════════════════════════════════════════
// 1. PRICING CATALOG — items, prices, coupons, tax codes, usage types
// ════════════════════════════════════════════════════════════════════════
const CATALOG_ITEMS = [{
  id: "itm_starter",
  name: "Starter plan",
  type: "plan",
  sku: "PLAN-STR",
  prices: 2,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "—",
  updated: "Aug 12"
}, {
  id: "itm_growth",
  name: "Growth plan",
  type: "plan",
  sku: "PLAN-GRW",
  prices: 4,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "—",
  updated: "Sep 02"
}, {
  id: "itm_enterprise",
  name: "Enterprise plan",
  type: "plan",
  sku: "PLAN-ENT",
  prices: 6,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "—",
  updated: "Sep 28"
}, {
  id: "itm_seat",
  name: "Additional seat",
  type: "addon",
  sku: "ADD-SEAT",
  prices: 3,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "Per seat",
  updated: "Jul 04"
}, {
  id: "itm_storage",
  name: "Storage block",
  type: "metered",
  sku: "MTR-STORAGE",
  prices: 2,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "Per 100 GB",
  updated: "May 20"
}, {
  id: "itm_api",
  name: "API calls",
  type: "metered",
  sku: "MTR-APICAL",
  prices: 1,
  taxCode: "EU-DIGITAL",
  active: true,
  usage: "Per 10k",
  updated: "Sep 14"
}, {
  id: "itm_onboarding",
  name: "Onboarding (one-off)",
  type: "service",
  sku: "SVC-ONB",
  prices: 2,
  taxCode: "NL-SERVICE",
  active: true,
  usage: "—",
  updated: "Mar 11"
}, {
  id: "itm_legacy",
  name: "Legacy data import",
  type: "service",
  sku: "SVC-IMP",
  prices: 1,
  taxCode: "NL-SERVICE",
  active: false,
  usage: "—",
  updated: "Jan 09"
}];
const COUPONS = [{
  id: "WELCOME20",
  name: "Welcome 20",
  kind: "percent",
  value: 20,
  duration: "Once",
  redemptions: 412,
  max: 1000,
  expires: "Dec 31, 2025",
  status: "active"
}, {
  id: "Q4PILOT",
  name: "Q4 pilot 50%",
  kind: "percent",
  value: 50,
  duration: "3 months",
  redemptions: 28,
  max: 50,
  expires: "Dec 15, 2025",
  status: "active"
}, {
  id: "ENT-NEG",
  name: "Enterprise neg.",
  kind: "amount-eur",
  value: 1500000,
  duration: "Once",
  redemptions: 4,
  max: 10,
  expires: "Mar 31, 2026",
  status: "active"
}, {
  id: "SUMMER24",
  name: "Summer 2024",
  kind: "percent",
  value: 15,
  duration: "Once",
  redemptions: 187,
  max: 200,
  expires: "Aug 31, 2024",
  status: "expired"
}, {
  id: "WINBACK",
  name: "Winback",
  kind: "percent",
  value: 25,
  duration: "6 months",
  redemptions: 0,
  max: null,
  expires: "—",
  status: "draft"
}];
const TAX_CODES = [{
  id: "EU-DIGITAL",
  name: "EU digital service",
  rate: "21%",
  jurisdiction: "EU member states · reverse-charge B2B",
  coverage: "Apps, plans, licenses"
}, {
  id: "NL-SERVICE",
  name: "NL professional service",
  rate: "21%",
  jurisdiction: "Netherlands · domestic",
  coverage: "Onboarding, training"
}, {
  id: "DE-RETAIL",
  name: "DE retail (19%)",
  rate: "19%",
  jurisdiction: "Germany",
  coverage: "Hardware add-ons"
}, {
  id: "ZERO",
  name: "Zero-rated export",
  rate: "0%",
  jurisdiction: "Outside EU",
  coverage: "Cross-border B2B"
}];
function PricingCatalogP({
  onOpenItem
}) {
  const [tab, setTab] = useStBE("items");
  const [q, setQ] = useStBE("");
  const [type, setType] = useStBE("all");
  const items = useMemoBE(() => CATALOG_ITEMS.filter(i => (type === "all" || i.type === type) && (!q || i.name.toLowerCase().includes(q.toLowerCase()) || i.sku.toLowerCase().includes(q.toLowerCase()))), [q, type]);
  const counts = {
    items: CATALOG_ITEMS.length,
    coupons: COUPONS.filter(c => c.status === "active").length,
    tax: TAX_CODES.length
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(BEHeader, {
    title: "Pricing catalog",
    subtitle: "Everything we sell \u2014 plans, add-ons, metered usage, services \u2014 and the prices, coupons and tax codes that wrap them.",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u2191"
      })
    }, "Import"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      })
    }, "New item"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(BEKpi, {
    label: "Catalog items",
    value: "8",
    delta: "6 active \xB7 2 hidden",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Active coupons",
    value: "3",
    delta: "444 redemptions YTD",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Tax codes",
    value: "4",
    delta: "EU + NL + zero-rated",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Last published",
    value: "Sep 28",
    delta: "by anna.krause@incedo",
    tone: "muted"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: tab,
    onChange: setTab,
    options: [{
      value: "items",
      label: "Items & prices",
      count: counts.items
    }, {
      value: "coupons",
      label: "Coupons",
      count: counts.coupons
    }, {
      value: "tax",
      label: "Tax codes",
      count: counts.tax
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), tab === "items" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("select", {
    value: type,
    onChange: e => setType(e.target.value),
    style: {
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: "6px 10px",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      font: "400 12px/16px Roboto",
      outline: "none"
    }
  }, /*#__PURE__*/React.createElement("option", {
    value: "all"
  }, "All types"), /*#__PURE__*/React.createElement("option", {
    value: "plan"
  }, "Plans"), /*#__PURE__*/React.createElement("option", {
    value: "addon"
  }, "Add-ons"), /*#__PURE__*/React.createElement("option", {
    value: "metered"
  }, "Metered"), /*#__PURE__*/React.createElement("option", {
    value: "service"
  }, "Services")), /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search SKU or name\u2026",
    value: q,
    onChange: setQ,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2315"
    }),
    style: {
      width: 240
    }
  }))), tab === "items" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.6fr 0.8fr 1fr 0.7fr 1fr 0.8fr 0.6fr 24px",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Name"), /*#__PURE__*/React.createElement("div", null, "Type"), /*#__PURE__*/React.createElement("div", null, "SKU"), /*#__PURE__*/React.createElement("div", null, "Prices"), /*#__PURE__*/React.createElement("div", null, "Tax"), /*#__PURE__*/React.createElement("div", null, "Usage"), /*#__PURE__*/React.createElement("div", null, "Status"), /*#__PURE__*/React.createElement("div", null)), items.map(i => /*#__PURE__*/React.createElement("div", {
    key: i.id,
    onClick: () => onOpenItem && onOpenItem(i),
    style: {
      display: "grid",
      gridTemplateColumns: "1.6fr 0.8fr 1fr 0.7fr 1fr 0.8fr 0.6fr 24px",
      alignItems: "center",
      padding: "12px 16px",
      cursor: "pointer",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-soft, rgba(0,0,0,.02))",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, i.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Updated ", i.updated)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: i.type === "plan" ? "info" : i.type === "metered" ? "plum" : i.type === "addon" ? "mint" : "amber",
    size: "sm"
  }, i.type)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)"
    }
  }, i.sku), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, i.prices), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, i.taxCode), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, i.usage), /*#__PURE__*/React.createElement("div", null, i.active ? /*#__PURE__*/React.createElement(BEPill, {
    tone: "mint",
    size: "sm"
  }, "Active") : /*#__PURE__*/React.createElement(BEPill, {
    tone: "muted",
    size: "sm"
  }, "Hidden")), /*#__PURE__*/React.createElement(BERowChev, null)))), tab === "coupons" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1.6fr 0.9fr 1fr 1fr 1fr 0.7fr",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Code"), /*#__PURE__*/React.createElement("div", null, "Name"), /*#__PURE__*/React.createElement("div", null, "Discount"), /*#__PURE__*/React.createElement("div", null, "Duration"), /*#__PURE__*/React.createElement("div", null, "Used"), /*#__PURE__*/React.createElement("div", null, "Expires"), /*#__PURE__*/React.createElement("div", null, "Status")), COUPONS.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1.6fr 0.9fr 1fr 1fr 1fr 0.7fr",
      alignItems: "center",
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, c.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, c.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, c.kind === "percent" ? `${c.value}%` : beEur(c.value, {
    dec: 0
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.duration), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.redemptions, c.max ? ` / ${c.max}` : ""), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.expires), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: c.status === "active" ? "mint" : c.status === "draft" ? "muted" : "warm",
    size: "sm"
  }, c.status))))), tab === "tax" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(2, 1fr)",
      gap: 12
    }
  }, TAX_CODES.map(t => /*#__PURE__*/React.createElement(PCard, {
    key: t.id,
    pad: 18
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)"
    }
  }, t.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)",
      marginTop: 2
    }
  }, t.name)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 22px/26px Roboto",
      color: "var(--sp-text)"
    }
  }, t.rate)), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, t.jurisdiction), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 4,
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, t.coverage)))));
}

// ════════════════════════════════════════════════════════════════════════
// 2. PAYMENTS LIST — txn-level history
// ════════════════════════════════════════════════════════════════════════
const PAYMENTS = [{
  id: "py_K3aF2",
  invoice: "INV-20251001",
  customer: "Acme Holdings",
  amount: 4990000,
  fee: 1497,
  net: 4988503,
  method: "Visa •• 4242",
  status: "succeeded",
  at: "Today, 11:42",
  channel: "card"
}, {
  id: "py_K3a2B",
  invoice: "INV-20250998",
  customer: "Orbit Labs",
  amount: 1990000,
  fee: 597,
  net: 1989403,
  method: "Mastercard •• 8822",
  status: "succeeded",
  at: "Today, 09:18",
  channel: "card"
}, {
  id: "py_K398q",
  invoice: "INV-20251003",
  customer: "Northwind GmbH",
  amount: 3840000,
  fee: 0,
  net: 0,
  method: "Visa •• 1119",
  status: "failed",
  at: "Today, 08:01",
  channel: "card",
  error: "insufficient_funds"
}, {
  id: "py_K38hN",
  invoice: "INV-20250995",
  customer: "Hanzeborg NV",
  amount: 1680000,
  fee: 320,
  net: 1679680,
  method: "SEPA •• 8411",
  status: "settled",
  at: "Yday, 17:33",
  channel: "sepa"
}, {
  id: "py_K38a1",
  invoice: "INV-20250991",
  customer: "Lumen Studios",
  amount: 740000,
  fee: 222,
  net: 739778,
  method: "Mastercard •• 1121",
  status: "succeeded",
  at: "Yday, 14:12",
  channel: "card"
}, {
  id: "py_K374x",
  invoice: "INV-20250980",
  customer: "Polder & Co",
  amount: 240000,
  fee: 0,
  net: -240000,
  method: "Visa •• 0042",
  status: "refunded",
  at: "Mon, 11:00",
  channel: "card",
  refundOf: "py_K2x1y"
}, {
  id: "py_K371q",
  invoice: "INV-20250972",
  customer: "Kairos Mobility",
  amount: 1890000,
  fee: 567,
  net: 1889433,
  method: "Visa •• 6611",
  status: "succeeded",
  at: "Mon, 09:45",
  channel: "card"
}, {
  id: "py_K36p2",
  invoice: "INV-20250963",
  customer: "Peregrine AI",
  amount: 49000,
  fee: 14,
  net: 48986,
  method: "Visa •• 5500",
  status: "succeeded",
  at: "Sun, 21:08",
  channel: "card"
}, {
  id: "py_K35nq",
  invoice: "INV-20250958",
  customer: "Acme Holdings",
  amount: 4990000,
  fee: 1497,
  net: 4988503,
  method: "Visa •• 4242",
  status: "succeeded",
  at: "Sat, 11:00",
  channel: "card"
}, {
  id: "py_K34d1",
  invoice: "INV-20250949",
  customer: "Northwind GmbH",
  amount: 3840000,
  fee: 0,
  net: 0,
  method: "Visa •• 1119",
  status: "disputed",
  at: "Fri, 16:22",
  channel: "card",
  error: "chargeback_initiated"
}];
function PaymentsListP({
  onOpenInvoice
}) {
  const [status, setStatus] = useStBE("all");
  const [q, setQ] = useStBE("");
  const [active, setActive] = useStBE(null);
  const filtered = PAYMENTS.filter(p => (status === "all" || p.status === status) && (!q || p.customer.toLowerCase().includes(q.toLowerCase()) || p.invoice.toLowerCase().includes(q.toLowerCase()) || p.id.toLowerCase().includes(q.toLowerCase())));
  const totals = {
    gross: PAYMENTS.filter(p => p.status === "succeeded" || p.status === "settled").reduce((a, b) => a + b.amount, 0),
    fees: PAYMENTS.reduce((a, b) => a + b.fee, 0),
    refunds: PAYMENTS.filter(p => p.status === "refunded").reduce((a, b) => a + Math.abs(b.net), 0),
    failed: PAYMENTS.filter(p => p.status === "failed" || p.status === "disputed").length
  };
  const statusTone = s => s === "succeeded" ? "mint" : s === "settled" ? "info" : s === "failed" ? "warm" : s === "refunded" ? "muted" : s === "disputed" ? "amber" : "muted";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(BEHeader, {
    title: "Payments",
    subtitle: "Every charge, refund and chargeback. Filter by status, drill into a payment for the receipt and timeline.",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Export CSV"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      })
    }, "Manual payment"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(BEKpi, {
    label: "Gross collected \xB7 7d",
    value: beEur(totals.gross),
    delta: "\u2191 12% vs prior week",
    tone: "up"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Processor fees \xB7 7d",
    value: beEur(totals.fees),
    delta: "0.3% effective rate",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Refunds issued \xB7 7d",
    value: beEur(totals.refunds),
    delta: "1 refund",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Failed / disputed",
    value: String(totals.failed),
    delta: "2 require action",
    tone: "down"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: status,
    onChange: setStatus,
    options: [{
      value: "all",
      label: "All",
      count: PAYMENTS.length
    }, {
      value: "succeeded",
      label: "Succeeded"
    }, {
      value: "settled",
      label: "Settled"
    }, {
      value: "failed",
      label: "Failed"
    }, {
      value: "refunded",
      label: "Refunded"
    }, {
      value: "disputed",
      label: "Disputed"
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search by customer, invoice, py_id\u2026",
    value: q,
    onChange: setQ,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2315"
    }),
    style: {
      width: 280
    }
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1.2fr 1.4fr 1fr 1.2fr 0.8fr 1fr 24px",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Payment"), /*#__PURE__*/React.createElement("div", null, "When"), /*#__PURE__*/React.createElement("div", null, "Customer"), /*#__PURE__*/React.createElement("div", null, "Invoice"), /*#__PURE__*/React.createElement("div", null, "Method"), /*#__PURE__*/React.createElement("div", null, "Amount"), /*#__PURE__*/React.createElement("div", null, "Status"), /*#__PURE__*/React.createElement("div", null)), filtered.map(p => /*#__PURE__*/React.createElement("div", {
    key: p.id,
    onClick: () => setActive(p),
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1.2fr 1.4fr 1fr 1.2fr 0.8fr 1fr 24px",
      alignItems: "center",
      padding: "12px 16px",
      cursor: "pointer",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))",
      background: active?.id === p.id ? "var(--sp-surface-soft, rgba(26,115,232,.04))" : "transparent"
    },
    onMouseEnter: e => {
      if (active?.id !== p.id) e.currentTarget.style.background = "var(--sp-surface-soft, rgba(0,0,0,.02))";
    },
    onMouseLeave: e => {
      if (active?.id !== p.id) e.currentTarget.style.background = "transparent";
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, p.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, p.at), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, p.customer), /*#__PURE__*/React.createElement("div", {
    onClick: e => {
      e.stopPropagation();
      onOpenInvoice && onOpenInvoice({
        id: p.invoice,
        customer: p.customer,
        amount: p.amount,
        status: "paid"
      });
    },
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "#1A73E8",
      textDecoration: "underline",
      cursor: "pointer"
    }
  }, p.invoice), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, p.method), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: p.status === "refunded" ? "#D93025" : "var(--sp-text)"
    }
  }, p.status === "refunded" ? `−${beEur(Math.abs(p.net))}` : beEur(p.amount)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: statusTone(p.status),
    size: "sm"
  }, p.status)), /*#__PURE__*/React.createElement(BERowChev, null))), filtered.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, "No payments match those filters.")), /*#__PURE__*/React.createElement(PDrawer, {
    open: !!active,
    onClose: () => setActive(null),
    title: active ? `Payment · ${active.id}` : "",
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: () => setActive(null)
    }, "Close"), active?.status === "succeeded" && /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A9"
      })
    }, "Issue refund"), active?.status === "failed" && /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u27F3"
      })
    }, "Retry now"))
  }, active && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: active.status === "refunded" ? "#D93025" : "var(--sp-text)"
    }
  }, active.status === "refunded" ? `−${beEur(Math.abs(active.net))}` : beEur(active.amount)), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6,
      display: "flex",
      gap: 8,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(BEPill, {
    tone: statusTone(active.status),
    size: "sm"
  }, active.status), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "\xB7 ", active.at)), active.error && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      padding: "8px 10px",
      background: "rgba(217,48,37,.08)",
      color: "#D93025",
      borderRadius: 6,
      font: "500 12px/16px 'Roboto Mono', monospace"
    }
  }, active.error)), /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "Receipt"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 10,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Customer"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, active.customer)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Invoice"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px 'Roboto Mono', monospace",
      color: "#1A73E8"
    }
  }, active.invoice)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Method"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, active.method)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Channel"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, active.channel === "sepa" ? "SEPA Direct Debit" : "Card / Stripe")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Processor fee"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(active.fee))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Net to bank"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(active.net))))), /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "Timeline"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, [{
    at: active.at,
    msg: active.status === "succeeded" ? "Charge succeeded" : active.status === "failed" ? "Charge failed" : active.status === "refunded" ? "Refund issued" : active.status === "settled" ? "Settled to bank" : "Chargeback initiated",
    icon: active.status === "failed" ? "✕" : active.status === "refunded" ? "↩" : "✓"
  }, {
    at: "—",
    msg: "Charge attempted on " + active.method,
    icon: "▣"
  }, {
    at: "—",
    msg: `Invoice ${active.invoice} marked due`,
    icon: "✎"
  }].map((e, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 10,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 24,
      height: 24,
      borderRadius: 12,
      background: "var(--sp-surface-soft, rgba(0,0,0,.06))",
      display: "grid",
      placeItems: "center",
      color: "var(--sp-text-muted)"
    }
  }, e.icon), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, e.msg), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, e.at)))))))));
}

// ════════════════════════════════════════════════════════════════════════
// 3. SEPA — collection runs + mandates
// ════════════════════════════════════════════════════════════════════════
const SEPA_RUNS = [{
  id: "RUN-2025-10-15",
  date: "Oct 15, 2025",
  txns: 184,
  gross: 14820000,
  status: "settled",
  processed: "Oct 17",
  returns: 3,
  pain: "pain.008.001.08"
}, {
  id: "RUN-2025-10-01",
  date: "Oct 1, 2025",
  txns: 192,
  gross: 15240000,
  status: "settled",
  processed: "Oct 3",
  returns: 5,
  pain: "pain.008.001.08"
}, {
  id: "RUN-2025-09-15",
  date: "Sep 15, 2025",
  txns: 178,
  gross: 14110000,
  status: "settled",
  processed: "Sep 17",
  returns: 2,
  pain: "pain.008.001.08"
}, {
  id: "RUN-2025-10-29",
  date: "Oct 29, 2025",
  txns: 192,
  gross: 15600000,
  status: "scheduled",
  processed: "—",
  returns: 0,
  pain: "pain.008.001.08"
}, {
  id: "RUN-2025-11-15",
  date: "Nov 15, 2025",
  txns: 198,
  gross: 16110000,
  status: "draft",
  processed: "—",
  returns: 0,
  pain: "pain.008.001.08"
}];
const MANDATES = [{
  id: "MND-AC-001",
  customer: "Acme Holdings",
  iban: "NL44 INGB 0123 4567 89",
  signed: "Feb 12, 2024",
  scheme: "B2B",
  status: "active",
  used: 18
}, {
  id: "MND-OR-002",
  customer: "Orbit Labs",
  iban: "NL21 RABO 9876 5432 10",
  signed: "Mar 04, 2024",
  scheme: "CORE",
  status: "active",
  used: 14
}, {
  id: "MND-NW-003",
  customer: "Northwind GmbH",
  iban: "DE89 3704 0044 0532 0130 00",
  signed: "Jan 30, 2024",
  scheme: "B2B",
  status: "suspended",
  used: 22
}, {
  id: "MND-HZ-004",
  customer: "Hanzeborg NV",
  iban: "NL66 ABNA 0411 7651 22",
  signed: "Apr 18, 2024",
  scheme: "CORE",
  status: "active",
  used: 11
}, {
  id: "MND-PC-005",
  customer: "Polder & Co",
  iban: "NL71 BUNQ 9888 1122 33",
  signed: "Aug 11, 2024",
  scheme: "CORE",
  status: "pending",
  used: 0
}, {
  id: "MND-LU-006",
  customer: "Lumen Studios",
  iban: "NL11 INGB 0007 7166 88",
  signed: "May 22, 2024",
  scheme: "CORE",
  status: "active",
  used: 7
}, {
  id: "MND-KM-007",
  customer: "Kairos Mobility",
  iban: "NL35 RABO 0124 8800 91",
  signed: "Jun 03, 2024",
  scheme: "CORE",
  status: "revoked",
  used: 9
}];
function SepaP() {
  const [tab, setTab] = useStBE("runs");
  const [active, setActive] = useStBE(null);
  const upcomingRun = SEPA_RUNS.find(r => r.status === "scheduled");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(BEHeader, {
    title: "SEPA",
    subtitle: "Direct-debit collection runs and mandates. Generates pain.008 files on the schedule, tracks returns, and updates mandate state from camt.054 reports.",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Download last pain.008"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u25B6"
      })
    }, "New collection run"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(BEKpi, {
    label: "Active mandates",
    value: "184",
    delta: "3 pending \xB7 1 suspended",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Next run",
    value: upcomingRun?.date || "—",
    delta: upcomingRun ? `${upcomingRun.txns} txns · ${beEur(upcomingRun.gross, {
      dec: 0
    })}` : "",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Returns \xB7 30d",
    value: "10",
    delta: "of 376 \u2014 2.7% return rate",
    tone: "down"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Net collected \xB7 30d",
    value: beEur(28930000),
    delta: "\u2191 4% vs prior period",
    tone: "up"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: tab,
    onChange: setTab,
    options: [{
      value: "runs",
      label: "Collection runs",
      count: SEPA_RUNS.length
    }, {
      value: "mandates",
      label: "Mandates",
      count: MANDATES.length
    }]
  })), tab === "runs" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1fr 0.8fr 1fr 0.8fr 0.7fr 1fr 24px",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Run ID"), /*#__PURE__*/React.createElement("div", null, "Collection date"), /*#__PURE__*/React.createElement("div", null, "Txns"), /*#__PURE__*/React.createElement("div", null, "Gross"), /*#__PURE__*/React.createElement("div", null, "Returns"), /*#__PURE__*/React.createElement("div", null, "Format"), /*#__PURE__*/React.createElement("div", null, "Status"), /*#__PURE__*/React.createElement("div", null)), SEPA_RUNS.map(r => /*#__PURE__*/React.createElement("div", {
    key: r.id,
    onClick: () => setActive({
      kind: "run",
      data: r
    }),
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1fr 0.8fr 1fr 0.8fr 0.7fr 1fr 24px",
      alignItems: "center",
      padding: "12px 16px",
      cursor: "pointer",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, r.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, r.date), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, r.txns), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(r.gross, {
    dec: 0
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: r.returns > 0 ? "#D93025" : "var(--sp-text-muted)"
    }
  }, r.returns), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)"
    }
  }, r.pain), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: r.status === "settled" ? "mint" : r.status === "scheduled" ? "info" : "muted",
    size: "sm"
  }, r.status)), /*#__PURE__*/React.createElement(BERowChev, null)))), tab === "mandates" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.2fr 1.4fr 1.6fr 1fr 0.8fr 0.7fr 0.8fr",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Mandate"), /*#__PURE__*/React.createElement("div", null, "Customer"), /*#__PURE__*/React.createElement("div", null, "IBAN"), /*#__PURE__*/React.createElement("div", null, "Signed"), /*#__PURE__*/React.createElement("div", null, "Scheme"), /*#__PURE__*/React.createElement("div", null, "Used"), /*#__PURE__*/React.createElement("div", null, "Status")), MANDATES.map(m => /*#__PURE__*/React.createElement("div", {
    key: m.id,
    style: {
      display: "grid",
      gridTemplateColumns: "1.2fr 1.4fr 1.6fr 1fr 0.8fr 0.7fr 0.8fr",
      alignItems: "center",
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, m.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, m.customer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)"
    }
  }, m.iban), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, m.signed), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, m.scheme), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, m.used), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: m.status === "active" ? "mint" : m.status === "pending" ? "info" : m.status === "suspended" ? "amber" : "warm",
    size: "sm"
  }, m.status))))), /*#__PURE__*/React.createElement(PDrawer, {
    open: !!active,
    onClose: () => setActive(null),
    title: active?.kind === "run" ? `Collection run · ${active.data.id}` : "",
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: () => setActive(null)
    }, "Close"), active?.data?.status === "settled" && /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Download camt.054"), active?.data?.status === "draft" && /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u25B6"
      })
    }, "Schedule run"))
  }, active?.kind === "run" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "Summary"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 12,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Collection date"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, active.data.date)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Processed"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, active.data.processed)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Transactions"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, active.data.txns)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Gross"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 18px/24px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(active.data.gross, {
    dec: 0
  }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Returns"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: active.data.returns > 0 ? "#D93025" : "var(--sp-text)"
    }
  }, active.data.returns)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Format"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, active.data.pain)))), /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "Files"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, [{
    name: `${active.data.id}.pain.008.xml`,
    kind: "pain.008",
    size: "184 kB"
  }, active.data.status === "settled" ? {
    name: `${active.data.id}.camt.054.xml`,
    kind: "camt.054",
    size: "92 kB"
  } : null, active.data.status === "settled" && active.data.returns > 0 ? {
    name: `${active.data.id}.returns.csv`,
    kind: "returns",
    size: "2 kB"
  } : null].filter(Boolean).map((f, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "8px 10px",
      border: "1px solid var(--sp-border)",
      borderRadius: 6
    }
  }, /*#__PURE__*/React.createElement(Ico, {
    g: "\u25F0"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, f.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, f.kind, " \xB7 ", f.size)), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A7"
    })
  }, "Download"))))))));
}

// ════════════════════════════════════════════════════════════════════════
// 4. LEDGER — MRR/ARR/recognition + GL exports + reconciliation
// ════════════════════════════════════════════════════════════════════════
const GL_EXPORTS = [{
  id: "GL-2025-09",
  period: "Sep 2025",
  system: "Exact Online",
  format: "XAF",
  rows: 4218,
  status: "posted",
  at: "Oct 02, 2025"
}, {
  id: "GL-2025-10P",
  period: "Oct 2025",
  system: "Exact Online",
  format: "XAF",
  rows: 4640,
  status: "preview",
  at: "—"
}, {
  id: "GL-2025-08",
  period: "Aug 2025",
  system: "Exact Online",
  format: "XAF",
  rows: 4011,
  status: "posted",
  at: "Sep 03, 2025"
}, {
  id: "GL-2025-09Q",
  period: "Q3 2025",
  system: "Tableau Cube",
  format: "CSV",
  rows: 12289,
  status: "posted",
  at: "Oct 02, 2025"
}];
const RECON_JOBS = [{
  id: "RC-1041",
  source: "Stripe payouts",
  period: "Oct 14–15",
  expected: 4880000,
  matched: 4880000,
  diff: 0,
  status: "ok"
}, {
  id: "RC-1040",
  source: "ING bank statement",
  period: "Oct 14",
  expected: 14820000,
  matched: 14820000,
  diff: 0,
  status: "ok"
}, {
  id: "RC-1039",
  source: "Stripe payouts",
  period: "Oct 13",
  expected: 1989403,
  matched: 1980000,
  diff: 9403,
  status: "diff"
}, {
  id: "RC-1038",
  source: "Adyen settlement",
  period: "Oct 12–13",
  expected: 7300000,
  matched: 7300000,
  diff: 0,
  status: "ok"
}, {
  id: "RC-1037",
  source: "ING bank statement",
  period: "Oct 11",
  expected: 230000,
  matched: 0,
  diff: 230000,
  status: "miss"
}];
function LedgerP() {
  const [tab, setTab] = useStBE("rev");
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(BEHeader, {
    title: "Ledger",
    subtitle: "Revenue recognition, GL exports and bank reconciliation. The hand-off between Subscriptions and the finance system.",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Export period close"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u25B6"
      })
    }, "Run reconciliation"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(BEKpi, {
    label: "MRR \xB7 live",
    value: beEur(72870000, {
      dec: 0
    }),
    delta: "\u2191 5.7% MoM",
    tone: "up"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "ARR \xB7 run-rate",
    value: beEur(874440000, {
      dec: 0
    }),
    delta: "184 customers",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Net new MRR \xB7 this month",
    value: beEur(3945000, {
      dec: 0
    }),
    delta: "+\u20AC42k from upgrades \xB7 \u2212\u20AC8k from churn",
    tone: "up"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Deferred revenue",
    value: beEur(38120000, {
      dec: 0
    }),
    delta: "recognized over avg. 11.3 months",
    tone: "muted"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: tab,
    onChange: setTab,
    options: [{
      value: "rev",
      label: "Revenue movement"
    }, {
      value: "exp",
      label: "GL exports",
      count: GL_EXPORTS.length
    }, {
      value: "recon",
      label: "Reconciliation",
      count: RECON_JOBS.length
    }]
  })), tab === "rev" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 20
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "MRR \u2014 last 12 months (\u20ACk)"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-end",
      gap: 10,
      height: 200,
      marginTop: 18
    }
  }, MRR_SERIES.map((v, i) => {
    const max = Math.max(...MRR_SERIES);
    const h = v / max * 180;
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        flex: 1,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 6
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 10px/12px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, v), /*#__PURE__*/React.createElement("div", {
      style: {
        width: "100%",
        height: h,
        background: "linear-gradient(180deg, #1A73E8 0%, #1557B0 100%)",
        borderRadius: "4px 4px 0 0",
        minHeight: 4
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 10px/12px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, ["N", "D", "J", "F", "M", "A", "M", "J", "J", "A", "S", "O"][i]));
  }))), /*#__PURE__*/React.createElement(PCard, {
    pad: 20
  }, /*#__PURE__*/React.createElement(BESideLabel, null, "Movement \xB7 this month"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10,
      marginTop: 14
    }
  }, [{
    l: "Starting MRR",
    v: beEur(68920000, {
      dec: 0
    }),
    tone: "muted"
  }, {
    l: "+ New",
    v: "+ " + beEur(2840000, {
      dec: 0
    }),
    tone: "up"
  }, {
    l: "+ Expansion",
    v: "+ " + beEur(1620000, {
      dec: 0
    }),
    tone: "up"
  }, {
    l: "− Contraction",
    v: "− " + beEur(310000, {
      dec: 0
    }),
    tone: "down"
  }, {
    l: "− Churn",
    v: "− " + beEur(205000, {
      dec: 0
    }),
    tone: "down"
  }].map((r, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, r.l), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px Roboto",
      color: r.tone === "up" ? "#108C6B" : r.tone === "down" ? "#D93025" : "var(--sp-text)"
    }
  }, r.v))), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--sp-border)",
      paddingTop: 10,
      marginTop: 4,
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Ending MRR"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "700 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(72865000, {
    dec: 0
  })))))), tab === "exp" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1.2fr 0.8fr 0.7fr 1fr 0.8fr",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Export"), /*#__PURE__*/React.createElement("div", null, "Period"), /*#__PURE__*/React.createElement("div", null, "System"), /*#__PURE__*/React.createElement("div", null, "Format"), /*#__PURE__*/React.createElement("div", null, "Rows"), /*#__PURE__*/React.createElement("div", null, "Posted"), /*#__PURE__*/React.createElement("div", null, "Status")), GL_EXPORTS.map(g => /*#__PURE__*/React.createElement("div", {
    key: g.id,
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1.2fr 0.8fr 0.7fr 1fr 0.8fr",
      alignItems: "center",
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, g.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, g.period), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, g.system), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: "info",
    size: "sm"
  }, g.format)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, g.rows.toLocaleString()), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, g.at), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: g.status === "posted" ? "mint" : "amber",
    size: "sm"
  }, g.status))))), tab === "recon" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1.4fr 1fr 1fr 1fr 0.8fr 0.7fr",
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Job"), /*#__PURE__*/React.createElement("div", null, "Source"), /*#__PURE__*/React.createElement("div", null, "Period"), /*#__PURE__*/React.createElement("div", null, "Expected"), /*#__PURE__*/React.createElement("div", null, "Matched"), /*#__PURE__*/React.createElement("div", null, "Diff"), /*#__PURE__*/React.createElement("div", null, "Status")), RECON_JOBS.map(r => /*#__PURE__*/React.createElement("div", {
    key: r.id,
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1.4fr 1fr 1fr 1fr 0.8fr 0.7fr",
      alignItems: "center",
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "var(--sp-text)"
    }
  }, r.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, r.source), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, r.period), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(r.expected, {
    dec: 0
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, beEur(r.matched, {
    dec: 0
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: r.diff !== 0 ? "#D93025" : "var(--sp-text-muted)"
    }
  }, r.diff === 0 ? "—" : beEur(r.diff, {
    dec: 0
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(BEPill, {
    tone: r.status === "ok" ? "mint" : r.status === "diff" ? "amber" : "warm",
    size: "sm"
  }, r.status === "ok" ? "matched" : r.status === "diff" ? "diff" : "missing"))))));
}

// ════════════════════════════════════════════════════════════════════════
// 5. TEMPLATES — notification templates list + edit/preview
// ════════════════════════════════════════════════════════════════════════
const TEMPLATES = [{
  id: "tpl_inv_issued",
  name: "Invoice issued",
  channel: "email",
  lang: "en",
  trigger: "invoice.created",
  updated: "Sep 30",
  subject: "Your Incedo invoice {{invoice.id}} is ready",
  active: true
}, {
  id: "tpl_inv_failed",
  name: "Payment failed",
  channel: "email",
  lang: "en",
  trigger: "payment.failed",
  updated: "Oct 12",
  subject: "We couldn't charge {{customer.name}}",
  active: true
}, {
  id: "tpl_inv_succ",
  name: "Payment received",
  channel: "email",
  lang: "en",
  trigger: "payment.succeeded",
  updated: "Aug 14",
  subject: "Thanks — payment received for {{invoice.id}}",
  active: true
}, {
  id: "tpl_dun_d3",
  name: "Dunning · day 3",
  channel: "email",
  lang: "en",
  trigger: "dunning.step3",
  updated: "Sep 04",
  subject: "Action needed — invoice {{invoice.id}} overdue",
  active: true
}, {
  id: "tpl_dun_d10",
  name: "Dunning · day 10",
  channel: "email",
  lang: "en",
  trigger: "dunning.step10",
  updated: "Sep 04",
  subject: "Final notice — invoice {{invoice.id}}",
  active: true
}, {
  id: "tpl_renew_30",
  name: "Renewal in 30 days",
  channel: "email",
  lang: "en",
  trigger: "renewal.t-30",
  updated: "Jun 22",
  subject: "Your subscription renews on {{sub.next}}",
  active: true
}, {
  id: "tpl_trial_ending",
  name: "Trial ending",
  channel: "email",
  lang: "en",
  trigger: "trial.ending",
  updated: "Jul 11",
  subject: "Your trial ends in 3 days",
  active: true
}, {
  id: "tpl_inv_failed_nl",
  name: "Betaling mislukt (NL)",
  channel: "email",
  lang: "nl",
  trigger: "payment.failed",
  updated: "Oct 12",
  subject: "Het is niet gelukt {{customer.name}} te incasseren",
  active: true
}, {
  id: "tpl_dun_sms",
  name: "Dunning · SMS reminder",
  channel: "sms",
  lang: "en",
  trigger: "dunning.step3",
  updated: "Aug 02",
  subject: "—",
  active: false
}];
function TemplatesP() {
  const [active, setActive] = useStBE(null);
  const [filter, setFilter] = useStBE("all");
  const [designing, setDesigning] = useStBE(null); // template being visually designed (or "new")

  if (designing) {
    const base = blankDoc();
    const initialDoc = designing === "new" || !designing.doc ? {
      ...base,
      name: designing === "new" ? "New template" : designing.name,
      subject: designing === "new" ? "Subject line" : designing.subject
    } : {
      ...base,
      ...designing.doc
    };
    return /*#__PURE__*/React.createElement(TemplateDesigner, {
      initialDoc: initialDoc,
      onCancel: () => setDesigning(null),
      onSave: d => {
        setDesigning(null);
        setActive({
          ...(designing === "new" ? {
            id: "tpl_new",
            channel: "email",
            lang: "en",
            trigger: "—",
            active: true
          } : designing),
          name: d.name,
          subject: d.subject,
          doc: d
        });
      }
    });
  }
  const filtered = TEMPLATES.filter(t => filter === "all" || t.channel === filter);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(BEHeader, {
    title: "Notification templates",
    subtitle: "Email and SMS the system sends \u2014 invoices, dunning, renewals. Edit copy, swap merge tags, preview against a real customer.",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u25EB"
      })
    }, "Test send"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      size: "sm",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      }),
      onClick: () => setDesigning("new")
    }, "New template"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(BEKpi, {
    label: "Active templates",
    value: "8",
    delta: "7 email \xB7 1 SMS",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Languages",
    value: "2",
    delta: "EN, NL",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Sent \xB7 30d",
    value: "3,840",
    delta: "open rate 62%",
    tone: "up"
  }), /*#__PURE__*/React.createElement(BEKpi, {
    label: "Bounce rate",
    value: "0.4%",
    delta: "below 1% target",
    tone: "up"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.2fr 2fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: filter,
    onChange: setFilter,
    options: [{
      value: "all",
      label: "All",
      count: TEMPLATES.length
    }, {
      value: "email",
      label: "Email"
    }, {
      value: "sms",
      label: "SMS"
    }]
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, filtered.map(t => {
    const on = active?.id === t.id;
    return /*#__PURE__*/React.createElement("div", {
      key: t.id,
      onClick: () => setActive(t),
      style: {
        padding: "12px 14px",
        cursor: "pointer",
        borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))",
        background: on ? "rgba(26,115,232,.06)" : "transparent",
        borderLeft: on ? "3px solid #1A73E8" : "3px solid transparent"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, t.name), !t.active && /*#__PURE__*/React.createElement(BEPill, {
      tone: "muted",
      size: "sm"
    }, "off")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/14px 'Roboto Mono', monospace",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, t.trigger), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 6,
        marginTop: 6
      }
    }, /*#__PURE__*/React.createElement(BEPill, {
      tone: t.channel === "email" ? "info" : "plum",
      size: "sm"
    }, t.channel), /*#__PURE__*/React.createElement(BEPill, {
      tone: "muted",
      size: "sm"
    }, t.lang.toUpperCase())));
  }))), /*#__PURE__*/React.createElement("div", null, !active && /*#__PURE__*/React.createElement(PCard, {
    pad: 48,
    style: {
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 36,
      color: "var(--sp-text-subtle)",
      marginBottom: 12
    }
  }, "\u2709"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Pick a template on the left to preview and edit.")), active && /*#__PURE__*/React.createElement(TemplatePreview, {
    tpl: active,
    onEdit: () => setDesigning(active)
  }))));
}
function TemplatePreview({
  tpl,
  onEdit
}) {
  const [tab, setTab] = useStBE("preview");
  const sampleBody = {
    tpl_inv_issued: `Hi {{customer.name}},\n\nYour invoice ${"{{invoice.id}}"} for ${"{{invoice.amount}}"} is ready.\n\nDue ${"{{invoice.due}}"}. We'll attempt to charge ${"{{invoice.method}}"} automatically — no action needed.\n\nView invoice → ${"{{invoice.url}}"}\n\n— Incedo Billing`,
    tpl_inv_failed: `Hi {{customer.name}},\n\nWe weren't able to charge ${"{{invoice.method}}"} for invoice ${"{{invoice.id}}"} (${"{{invoice.amount}}"}).\n\nReason: ${"{{payment.error}}"}\n\nWe'll retry automatically in 2 days. To pay now or update your method, head to ${"{{portal.url}}"}.\n\n— Incedo Billing`,
    tpl_inv_succ: `Thanks ${"{{customer.name}}"} — we received ${"{{invoice.amount}}"} for ${"{{invoice.id}}"}.\n\nReceipt: ${"{{invoice.receipt_url}}"}\n\n— Incedo Billing`,
    tpl_dun_d3: `Hi ${"{{customer.name}}"},\n\nInvoice ${"{{invoice.id}}"} (${"{{invoice.amount}}"}) is now 3 days overdue. To avoid service interruption, please pay or update your payment method by ${"{{dunning.deadline}}"}.\n\nPay now → ${"{{portal.url}}"}\n\n— Incedo Billing`,
    tpl_dun_d10: `Final notice for invoice ${"{{invoice.id}}"} (${"{{invoice.amount}}"}).\n\nIf this isn't resolved by ${"{{dunning.deadline}}"}, the subscription will be suspended and the case escalated to collections.\n\n${"{{portal.url}}"}`,
    tpl_renew_30: `Hi ${"{{customer.name}}"},\n\nYour ${"{{plan.name}}"} subscription renews on ${"{{sub.next}}"} for ${"{{sub.amount}}"}.\n\nManage seats or change plan: ${"{{portal.url}}"}\n\n— Incedo`,
    tpl_trial_ending: `Your Incedo trial ends in 3 days. Pick a plan to keep your data and team intact.\n\nChoose plan → ${"{{portal.url}}"}`,
    tpl_inv_failed_nl: `Hoi ${"{{customer.name}}"},\n\nHet is niet gelukt om ${"{{invoice.method}}"} te incasseren voor factuur ${"{{invoice.id}}"} (${"{{invoice.amount}}"}).\n\nReden: ${"{{payment.error}}"}\n\nWe proberen het over 2 dagen opnieuw. Direct betalen of betaalmethode wijzigen? ${"{{portal.url}}"}.\n\n— Incedo Billing`,
    tpl_dun_sms: `Incedo: invoice ${"{{invoice.id}}"} is overdue. Pay now ${"{{short.url}}"} to avoid suspension.`
  }[tpl.id] || "Template body…";
  return /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, tpl.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, tpl.id, " \xB7 trigger ", tpl.trigger)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u270E"
    }),
    onClick: onEdit
  }, "Edit in designer"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    })
  }, "Test send"))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "8px 16px",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(BETabs, {
    value: tab,
    onChange: setTab,
    options: [{
      value: "preview",
      label: "Preview"
    }, {
      value: "html",
      label: "HTML"
    }, {
      value: "vars",
      label: "Variables"
    }]
  })), tab === "preview" && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 20,
      background: "var(--sp-canvas)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 560,
      margin: "0 auto",
      background: "#fff",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      overflow: "hidden",
      boxShadow: "0 1px 2px rgba(0,0,0,.04)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 16px",
      background: "#1A73E8",
      color: "#fff",
      font: "700 16px/22px Roboto",
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 24,
      height: 24,
      borderRadius: 6,
      background: "rgba(255,255,255,.18)",
      display: "grid",
      placeItems: "center",
      fontSize: 12
    }
  }, "i"), "Incedo"), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16,
      color: "#202124",
      font: "400 13px/20px Roboto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "#5F6368",
      marginBottom: 4
    }
  }, "SUBJECT"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      marginBottom: 14
    }
  }, tpl.subject), /*#__PURE__*/React.createElement("pre", {
    style: {
      font: "400 13px/20px Roboto",
      whiteSpace: "pre-wrap",
      color: "#202124",
      margin: 0
    }
  }, sampleBody), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 18,
      paddingTop: 14,
      borderTop: "1px solid #E8EAED",
      font: "400 11px/16px Roboto",
      color: "#80868B"
    }
  }, "Incedo B.V. \xB7 Herengracht 124 \xB7 Amsterdam \xB7 Unsubscribe (admin emails are still required)")))), tab === "html" && /*#__PURE__*/React.createElement("pre", {
    style: {
      margin: 0,
      padding: 20,
      font: "400 12px/18px 'Roboto Mono', monospace",
      color: "var(--sp-text)",
      background: "var(--sp-canvas)",
      overflow: "auto",
      maxHeight: 480
    }
  }, `<mjml>
  <mj-body>
    <mj-section background-color="#1A73E8">
      <mj-column>
        <mj-text color="#fff" font-size="20px" font-weight="700">Incedo</mj-text>
      </mj-column>
    </mj-section>
    <mj-section>
      <mj-column>
        <mj-text font-size="16px" font-weight="600">${tpl.subject}</mj-text>
        <mj-text>${sampleBody.replace(/\n/g, "<br/>")}</mj-text>
        <mj-button href="{{portal.url}}">View invoice</mj-button>
      </mj-column>
    </mj-section>
  </mj-body>
</mjml>`), tab === "vars" && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      marginBottom: 10
    }
  }, "Available merge tags"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(2, 1fr)",
      gap: 6
    }
  }, [["{{customer.name}}", "Acme Holdings"], ["{{customer.email}}", "billing@acme.example"], ["{{invoice.id}}", "INV-20251001"], ["{{invoice.amount}}", "€49,900.00"], ["{{invoice.due}}", "Oct 28, 2025"], ["{{invoice.url}}", "portal.incedo.com/i/INV-…"], ["{{invoice.method}}", "Visa •• 4242"], ["{{payment.error}}", "insufficient_funds"], ["{{plan.name}}", "Growth"], ["{{sub.next}}", "Nov 12, 2025"], ["{{sub.amount}}", "€199.00"], ["{{dunning.deadline}}", "Oct 25, 2025"], ["{{portal.url}}", "portal.incedo.com/u/abc"]].map(([k, v]) => /*#__PURE__*/React.createElement("div", {
    key: k,
    style: {
      padding: "8px 10px",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      display: "flex",
      justifyContent: "space-between",
      gap: 8,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono', monospace",
      color: "#1A73E8"
    }
  }, k), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, v))))));
}

// ════════════════════════════════════════════════════════════════════════
// Exports
// ════════════════════════════════════════════════════════════════════════
Object.assign(window, {
  PricingCatalogP,
  PaymentsListP,
  SepaP,
  LedgerP,
  TemplatesP,
  CATALOG_ITEMS,
  COUPONS,
  TAX_CODES,
  PAYMENTS,
  SEPA_RUNS,
  MANDATES,
  GL_EXPORTS,
  RECON_JOBS,
  TEMPLATES
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/billing_extras.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/billing_flows.jsx
try { (() => {
// Billing flows — change-plan, pause, cancel, reactivate, new-invoice, void, credit-note
// Each flow is a self-contained PModal component that takes (open, onClose, ctx, onConfirm).
// `ctx` carries whatever the screen knows (sub, invoice, customer name).
// `onConfirm` is invoked with the form payload so the host can fire a toast / refresh.

const {
  useState: useStBF,
  useMemo: useMemoBF
} = React;

// ─── shared bits ────────────────────────────────────────────────────────
const fEur = (cents, opts = {}) => ((cents || 0) / 100).toLocaleString("nl-NL", {
  style: "currency",
  currency: "EUR",
  maximumFractionDigits: opts.dec ?? 2,
  minimumFractionDigits: opts.dec ?? 2
});
function FRow({
  children,
  gap = 12
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap
    }
  }, children);
}
function FLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em"
    }
  }, children);
}
function FHelp({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, children);
}
function FCard({
  children,
  tone = "muted",
  style
}) {
  const bgs = {
    muted: "var(--sp-surface-soft, rgba(0,0,0,.03))",
    info: "rgba(26,115,232,.06)",
    warm: "rgba(217,48,37,.06)",
    mint: "rgba(16,140,107,.06)",
    amber: "rgba(196,138,28,.07)"
  };
  const fgs = {
    muted: "var(--sp-text-muted)",
    info: "#1A73E8",
    warm: "#D93025",
    mint: "#108C6B",
    amber: "#A2710C"
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: bgs[tone],
      borderRadius: 8,
      padding: 12,
      color: fgs[tone],
      font: "400 12px/18px Roboto",
      ...style
    }
  }, children);
}
function FRadio({
  value,
  options,
  onChange,
  columns = 1
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: `repeat(${columns}, 1fr)`,
      gap: 8
    }
  }, options.map(o => {
    const on = value === o.value;
    return /*#__PURE__*/React.createElement("button", {
      key: o.value,
      type: "button",
      onClick: () => onChange(o.value),
      style: {
        textAlign: "left",
        cursor: "pointer",
        border: on ? "2px solid #1A73E8" : "1px solid var(--sp-border)",
        borderRadius: 8,
        padding: on ? 11 : 12,
        background: on ? "rgba(26,115,232,.04)" : "var(--sp-surface)",
        color: "var(--sp-text)",
        outline: "none",
        display: "flex",
        flexDirection: "column",
        gap: 2
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, o.label), o.hint && /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/15px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, o.hint));
  }));
}
function FSelect({
  value,
  onChange,
  options
}) {
  return /*#__PURE__*/React.createElement("select", {
    value: value,
    onChange: e => onChange(e.target.value),
    style: {
      width: "100%",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: "10px 12px",
      font: "400 13px/18px Roboto",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      outline: "none"
    }
  }, options.map(o => /*#__PURE__*/React.createElement("option", {
    key: o.value,
    value: o.value
  }, o.label)));
}
function FTextarea({
  value,
  onChange,
  placeholder,
  rows = 3
}) {
  return /*#__PURE__*/React.createElement("textarea", {
    value: value,
    onChange: e => onChange(e.target.value),
    placeholder: placeholder,
    rows: rows,
    style: {
      width: "100%",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: "10px 12px",
      font: "400 13px/18px Roboto",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      outline: "none",
      resize: "vertical"
    }
  });
}

// ════════════════════════════════════════════════════════════════════════
// 1. CHANGE PLAN
// ════════════════════════════════════════════════════════════════════════
function ChangePlanFlow({
  open,
  onClose,
  sub,
  onConfirm
}) {
  const currentPlanId = sub?.plan?.toLowerCase().includes("enterprise") ? "enterprise" : sub?.plan?.toLowerCase().includes("growth") ? "growth" : "starter";
  const [planId, setPlanId] = useStBF(currentPlanId === "starter" ? "growth" : currentPlanId === "growth" ? "enterprise" : "growth");
  const [seats, setSeats] = useStBF(sub?.seats || 25);
  const [when, setWhen] = useStBF("now");
  const current = (window.PLANS || []).find(p => p.id === currentPlanId) || {
    id: "growth",
    name: "Growth",
    price: 19900
  };
  const target = (window.PLANS || []).find(p => p.id === planId) || current;
  const isUp = target.price * seats > current.price * (sub?.seats || seats);
  const proration = useMemoBF(() => {
    const daysLeft = 14;
    const daysInCycle = 30;
    const factor = daysLeft / daysInCycle;
    const newAmt = target.price * seats;
    const oldAmt = current.price * (sub?.seats || seats);
    return Math.round((newAmt - oldAmt) * factor);
  }, [target, current, seats, sub]);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: "Change plan",
    width: 560,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onConfirm({
        plan: target.name,
        seats,
        when,
        proration
      })
    }, when === "now" ? "Apply change now" : "Schedule for next cycle"))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "info"
  }, /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, sub?.customer || "Customer"), " is currently on the ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, current.name), " plan with ", sub?.seats || seats, " seats \u2014 ", fEur(current.price * (sub?.seats || seats)), "/mo."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "New plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: planId,
    onChange: setPlanId,
    columns: 3,
    options: (window.PLANS || []).map(p => ({
      value: p.id,
      label: p.name,
      hint: `${fEur(p.price)} ${p.interval}`
    }))
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Seats"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => setSeats(s => Math.max(1, s - 5))
  }, "\u22125"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => setSeats(s => Math.max(1, s - 1))
  }, "\u22121"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      textAlign: "center",
      font: "700 22px/28px Roboto",
      color: "var(--sp-text)"
    }
  }, seats), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => setSeats(s => s + 1)
  }, "+1"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => setSeats(s => s + 5)
  }, "+5")), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 6
    }
  }), /*#__PURE__*/React.createElement(FHelp, null, "Min 1 seat. Customer's current allocation is ", sub?.seats || "—", ".")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "When"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: when,
    onChange: setWhen,
    columns: 2,
    options: [{
      value: "now",
      label: "Apply now",
      hint: "Charge proration today"
    }, {
      value: "cycle",
      label: "Next billing cycle",
      hint: "On Nov 12 — no proration"
    }]
  })), /*#__PURE__*/React.createElement(FCard, {
    tone: isUp ? "info" : "amber"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", null, /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, target.name), " \xB7 ", seats, " seats \xB7 ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, fEur(target.price * seats), "/mo"), " from ", when === "now" ? "today" : "Nov 12", ".")), when === "now" && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      paddingTop: 8,
      borderTop: "1px dashed currentColor",
      color: "var(--sp-text)"
    }
  }, "Today's proration: ", /*#__PURE__*/React.createElement("strong", null, proration >= 0 ? "+" : "−", fEur(Math.abs(proration))), " on ", sub?.method || "Visa •• 4242"))));
}

// ════════════════════════════════════════════════════════════════════════
// 2. PAUSE
// ════════════════════════════════════════════════════════════════════════
function PauseFlow({
  open,
  onClose,
  sub,
  onConfirm
}) {
  const [duration, setDuration] = useStBF("30");
  const [keepData, setKeepData] = useStBF(true);
  const resumeDate = useMemoBF(() => {
    const d = new Date();
    d.setDate(d.getDate() + parseInt(duration || "30", 10));
    return d.toLocaleDateString("en-GB", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    });
  }, [duration]);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: "Pause subscription",
    width: 520,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Keep active"), /*#__PURE__*/React.createElement(PButton, {
      variant: "warm",
      onClick: () => onConfirm({
        duration,
        resumeDate,
        keepData
      })
    }, duration === "indef" ? "Pause indefinitely" : `Pause until ${resumeDate}`))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "amber"
  }, "Pausing stops billing and disables seats. The customer keeps read-only access. They can resume at any time."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Pause duration"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: duration,
    onChange: setDuration,
    columns: 4,
    options: [{
      value: "30",
      label: "30 days"
    }, {
      value: "60",
      label: "60 days"
    }, {
      value: "90",
      label: "90 days"
    }, {
      value: "indef",
      label: "Until resumed"
    }]
  })), duration !== "indef" && /*#__PURE__*/React.createElement(FCard, {
    tone: "info"
  }, "Auto-resume on ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, resumeDate), " \u2014 billing restarts at ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, fEur(sub?.mrr || 19900), "/mo"), " on ", sub?.method || "Visa •• 4242", "."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Customer data"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: keepData ? "keep" : "archive",
    onChange: v => setKeepData(v === "keep"),
    columns: 2,
    options: [{
      value: "keep",
      label: "Keep all data",
      hint: "Resume in seconds"
    }, {
      value: "archive",
      label: "Archive after 30d",
      hint: "Restorable for 90 days"
    }]
  })), /*#__PURE__*/React.createElement(FCard, {
    tone: "muted"
  }, /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, "Customer sees:"), " \"Your account is paused \u2014 billing will resume on ", duration === "indef" ? "the date you choose" : resumeDate, ".\"")));
}

// ════════════════════════════════════════════════════════════════════════
// 3. CANCEL
// ════════════════════════════════════════════════════════════════════════
const CANCEL_REASONS = [{
  id: "too_expensive",
  label: "Too expensive",
  offer: "20% off for 3 months"
}, {
  id: "missing_feature",
  label: "Missing a feature",
  offer: "Beta access to upcoming feature"
}, {
  id: "switching",
  label: "Switching to a competitor",
  offer: "Custom price match"
}, {
  id: "not_using",
  label: "Not using it enough",
  offer: "Pause instead of cancel"
}, {
  id: "company_change",
  label: "Company change / shutdown",
  offer: null
}, {
  id: "bugs",
  label: "Too many bugs / outages",
  offer: "Direct line to support eng"
}, {
  id: "support",
  label: "Support too slow",
  offer: "Dedicated CSM upgrade"
}, {
  id: "moving_in_house",
  label: "Building it in-house",
  offer: null
}, {
  id: "merger",
  label: "Acquired / merged",
  offer: null
}, {
  id: "other",
  label: "Other",
  offer: null
}];
function CancelFlow({
  open,
  onClose,
  sub,
  onConfirm
}) {
  const [step, setStep] = useStBF(1);
  const [reason, setReason] = useStBF("too_expensive");
  const [feedback, setFeedback] = useStBF("");
  const [when, setWhen] = useStBF("period_end");
  const r = CANCEL_REASONS.find(x => x.id === reason);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: () => {
      setStep(1);
      onClose();
    },
    title: step === 1 ? "Cancel subscription · why?" : step === 2 ? "Try a save offer?" : "Confirm cancellation",
    width: 560,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, step === 1 && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Keep subscription"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => setStep(r?.offer ? 2 : 3)
    }, "Continue \u2192")), step === 2 && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: () => setStep(3)
    }, "Decline offer \xB7 cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onConfirm({
        outcome: "saved",
        offer: r.offer,
        reason,
        feedback
      })
    }, "Accept \xB7 keep customer")), step === 3 && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: () => setStep(1)
    }, "\u2190 Back"), /*#__PURE__*/React.createElement(PButton, {
      variant: "warm",
      onClick: () => onConfirm({
        outcome: "canceled",
        reason,
        feedback,
        when
      })
    }, when === "now" ? "Cancel immediately" : "Cancel at period end")))
  }, step === 1 && /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "muted"
  }, /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, sub?.customer), " \xB7 ", sub?.plan, " \xB7 ", fEur(sub?.mrr || 19900), "/mo since ", sub?.start || "Mar 2024", "."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Primary reason"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: reason,
    onChange: setReason,
    columns: 2,
    options: CANCEL_REASONS.map(c => ({
      value: c.id,
      label: c.label
    }))
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Anything else? (optional)"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FTextarea, {
    value: feedback,
    onChange: setFeedback,
    placeholder: "Free-text feedback for the CSM team\u2026",
    rows: 3
  }))), step === 2 && /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "mint",
    style: {
      padding: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "#108C6B",
      textTransform: "uppercase",
      letterSpacing: ".06em"
    }
  }, "Retention offer"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 22px/28px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, r.offer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, "Tailored for \"", r.label.toLowerCase(), "\". Apply now and the customer keeps everything they have today.")), /*#__PURE__*/React.createElement(FHelp, null, "If accepted, you'll be returned to the subscription with the offer applied. The customer is not yet notified.")), step === 3 && /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "End access"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: when,
    onChange: setWhen,
    columns: 2,
    options: [{
      value: "period_end",
      label: "End of billing period",
      hint: "On Nov 12 · no refund"
    }, {
      value: "now",
      label: "Immediately",
      hint: "Pro-rata refund issued"
    }]
  })), /*#__PURE__*/React.createElement(FCard, {
    tone: "warm"
  }, /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, "This action sends:"), /*#__PURE__*/React.createElement("ul", {
    style: {
      margin: "6px 0 0 16px",
      padding: 0
    }
  }, /*#__PURE__*/React.createElement("li", null, "Cancellation confirmation email to ", sub?.customer || "the customer"), /*#__PURE__*/React.createElement("li", null, "Notification to ", sub?.owner || "owner", " and Finance"), /*#__PURE__*/React.createElement("li", null, "Loss reason \"", r?.label, "\" to the win/loss dashboard")))));
}

// ════════════════════════════════════════════════════════════════════════
// 4. REACTIVATE
// ════════════════════════════════════════════════════════════════════════
function ReactivateFlow({
  open,
  onClose,
  sub,
  onConfirm
}) {
  const [chargeNow, setChargeNow] = useStBF(true);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: "Reactivate subscription",
    width: 480,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onConfirm({
        chargeNow
      })
    }, "Reactivate"))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 14
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "mint"
  }, "Restoring ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, sub?.customer || "the subscription"), " to ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, sub?.plan || "Growth"), " \xB7 ", sub?.seats || 25, " seats \xB7 ", fEur(sub?.mrr || 19900), "/mo."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Billing"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: chargeNow ? "now" : "next",
    onChange: v => setChargeNow(v === "now"),
    columns: 2,
    options: [{
      value: "now",
      label: "Charge today",
      hint: "Resets billing anchor"
    }, {
      value: "next",
      label: "Resume on original cycle",
      hint: "Next charge Nov 12"
    }]
  })), /*#__PURE__*/React.createElement(FHelp, null, "Customer's data, integrations and seats are exactly as they were when paused. Welcome-back email is sent automatically.")));
}

// ════════════════════════════════════════════════════════════════════════
// 5. NEW INVOICE
// ════════════════════════════════════════════════════════════════════════
const ITEM_PRESETS = [{
  id: "growth_seat",
  name: "Growth plan — additional seat",
  unit: 9900,
  desc: "Per seat / month"
}, {
  id: "ent_seat",
  name: "Enterprise plan — additional seat",
  unit: 19900,
  desc: "Per seat / month"
}, {
  id: "onboarding",
  name: "Onboarding (one-off)",
  unit: 250000,
  desc: "One-time setup"
}, {
  id: "training",
  name: "Admin training (per session)",
  unit: 75000,
  desc: "Half day, on-site or remote"
}, {
  id: "data",
  name: "Legacy data import",
  unit: 180000,
  desc: "Per source system"
}];
function NewInvoiceFlow({
  open,
  onClose,
  onConfirm
}) {
  const [customer, setCustomer] = useStBF("Acme Holdings");
  const [items, setItems] = useStBF([{
    id: 1,
    name: "Growth plan — Monthly",
    qty: 1,
    unit: 19900
  }]);
  const [due, setDue] = useStBF("net14");
  const [send, setSend] = useStBF("send");
  const addPreset = p => setItems(prev => [...prev, {
    id: Date.now(),
    name: p.name,
    qty: 1,
    unit: p.unit
  }]);
  const updateItem = (id, patch) => setItems(prev => prev.map(i => i.id === id ? {
    ...i,
    ...patch
  } : i));
  const removeItem = id => setItems(prev => prev.filter(i => i.id !== id));
  const subtotal = items.reduce((a, b) => a + b.qty * b.unit, 0);
  const vat = Math.round(subtotal * 0.21);
  const total = subtotal + vat;
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: "New invoice",
    width: 680,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      onClick: () => onConfirm({
        customer,
        items,
        due,
        send: "draft",
        total
      })
    }, "Save as draft"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onConfirm({
        customer,
        items,
        due,
        send,
        total
      })
    }, send === "send" ? "Create & send" : "Create"))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Bill to"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FSelect, {
    value: customer,
    onChange: setCustomer,
    options: [{
      value: "Acme Holdings",
      label: "Acme Holdings · NL · €49,900/mo"
    }, {
      value: "Orbit Labs",
      label: "Orbit Labs · NL · €19,900/mo"
    }, {
      value: "Lumen Studios",
      label: "Lumen Studios · NL · €7,400/mo"
    }, {
      value: "Northwind GmbH",
      label: "Northwind GmbH · DE · €38,400/mo"
    }, {
      value: "Hanzeborg NV",
      label: "Hanzeborg NV · NL · €16,800/mo"
    }, {
      value: "Polder & Co",
      label: "Polder & Co · NL · €2,400/mo"
    }]
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Due"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FSelect, {
    value: due,
    onChange: setDue,
    options: [{
      value: "due_now",
      label: "On receipt"
    }, {
      value: "net7",
      label: "Net 7"
    }, {
      value: "net14",
      label: "Net 14"
    }, {
      value: "net30",
      label: "Net 30"
    }]
  }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Line items"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "3fr 0.6fr 1fr 1fr 32px",
      padding: "8px 12px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Description"), /*#__PURE__*/React.createElement("div", null, "Qty"), /*#__PURE__*/React.createElement("div", null, "Unit"), /*#__PURE__*/React.createElement("div", null, "Total"), /*#__PURE__*/React.createElement("div", null)), items.map(it => /*#__PURE__*/React.createElement("div", {
    key: it.id,
    style: {
      display: "grid",
      gridTemplateColumns: "3fr 0.6fr 1fr 1fr 32px",
      padding: "8px 12px",
      alignItems: "center",
      borderBottom: "1px solid var(--sp-border-subtle, rgba(0,0,0,.04))",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("input", {
    value: it.name,
    onChange: e => updateItem(it.id, {
      name: e.target.value
    }),
    style: {
      border: "1px solid transparent",
      background: "transparent",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      outline: "none",
      padding: "4px 6px",
      borderRadius: 4
    }
  }), /*#__PURE__*/React.createElement("input", {
    type: "number",
    value: it.qty,
    onChange: e => updateItem(it.id, {
      qty: parseInt(e.target.value || "1", 10)
    }),
    style: {
      border: "1px solid var(--sp-border)",
      borderRadius: 4,
      padding: "4px 6px",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("input", {
    type: "number",
    value: it.unit / 100,
    onChange: e => updateItem(it.id, {
      unit: Math.round(parseFloat(e.target.value || "0") * 100)
    }),
    style: {
      border: "1px solid var(--sp-border)",
      borderRadius: 4,
      padding: "4px 6px",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, fEur(it.qty * it.unit)), /*#__PURE__*/React.createElement("button", {
    onClick: () => removeItem(it.id),
    style: {
      border: "none",
      background: "transparent",
      color: "var(--sp-text-muted)",
      cursor: "pointer",
      fontSize: 16
    }
  }, "\xD7")))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      flexWrap: "wrap",
      marginTop: 8
    }
  }, ITEM_PRESETS.map(p => /*#__PURE__*/React.createElement("button", {
    key: p.id,
    onClick: () => addPreset(p),
    style: {
      border: "1px dashed var(--sp-border)",
      borderRadius: 16,
      padding: "4px 10px",
      background: "transparent",
      color: "var(--sp-text-muted)",
      font: "400 12px/16px Roboto",
      cursor: "pointer"
    }
  }, "+ ", p.name)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 12,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Delivery"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: send,
    onChange: setSend,
    columns: 2,
    options: [{
      value: "send",
      label: "Email & charge"
    }, {
      value: "draft",
      label: "Save as draft"
    }]
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 14
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "Subtotal"), /*#__PURE__*/React.createElement("span", null, fEur(subtotal))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("span", null, "VAT 21%"), /*#__PURE__*/React.createElement("span", null, fEur(vat))), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 1,
      background: "var(--sp-border)",
      margin: "8px 0"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "700 18px/24px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "Total"), /*#__PURE__*/React.createElement("span", null, fEur(total)))))));
}

// ════════════════════════════════════════════════════════════════════════
// 6. VOID INVOICE
// ════════════════════════════════════════════════════════════════════════
const VOID_REASONS = [{
  id: "duplicate",
  label: "Duplicate of another invoice"
}, {
  id: "customer_cancel",
  label: "Customer canceled the order"
}, {
  id: "wrong_amount",
  label: "Wrong amount or items"
}, {
  id: "wrong_customer",
  label: "Issued to wrong customer"
}, {
  id: "fraud",
  label: "Suspected fraud"
}, {
  id: "other",
  label: "Other"
}];
function VoidInvoiceFlow({
  open,
  onClose,
  inv,
  onConfirm
}) {
  const [reason, setReason] = useStBF("duplicate");
  const [notify, setNotify] = useStBF(true);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: `Void invoice · ${inv?.id || ""}`,
    width: 500,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "warm",
      onClick: () => onConfirm({
        reason,
        notify
      })
    }, "Void invoice"))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "warm"
  }, "Voiding cannot be undone. The invoice will be marked ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, "void"), " and removed from collection. Any pending charge attempts are canceled."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Reason"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: reason,
    onChange: setReason,
    columns: 2,
    options: VOID_REASONS.map(v => ({
      value: v.id,
      label: v.label
    }))
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Customer notification"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: notify ? "yes" : "no",
    onChange: v => setNotify(v === "yes"),
    columns: 2,
    options: [{
      value: "yes",
      label: "Email customer",
      hint: "Voids invoice in their portal too"
    }, {
      value: "no",
      label: "Silent void",
      hint: "Internal only — Finance audit log"
    }]
  }))));
}

// ════════════════════════════════════════════════════════════════════════
// 7. CREDIT NOTE
// ════════════════════════════════════════════════════════════════════════
const CREDIT_REASONS = [{
  id: "service_outage",
  label: "Service outage / SLA credit"
}, {
  id: "billing_error",
  label: "Billing error"
}, {
  id: "goodwill",
  label: "Goodwill / retention"
}, {
  id: "downgrade",
  label: "Mid-cycle downgrade"
}, {
  id: "duplicate",
  label: "Duplicate charge"
}, {
  id: "other",
  label: "Other"
}];
function CreditNoteFlow({
  open,
  onClose,
  inv,
  onConfirm
}) {
  const [kind, setKind] = useStBF("partial");
  const [amount, setAmount] = useStBF(Math.round((inv?.amount || 19900) * 0.5));
  const [reason, setReason] = useStBF("service_outage");
  const [refund, setRefund] = useStBF("invoice");
  const [note, setNote] = useStBF("");
  const max = inv?.amount || 0;
  const finalAmt = kind === "full" ? max : Math.min(amount, max);
  return /*#__PURE__*/React.createElement(PModal, {
    open: open,
    onClose: onClose,
    title: `Issue credit note · ${inv?.id || ""}`,
    width: 560,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "ghost",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onConfirm({
        kind,
        amount: finalAmt,
        reason,
        refund,
        note
      })
    }, "Issue ", fEur(finalAmt), " credit"))
  }, /*#__PURE__*/React.createElement(FRow, {
    gap: 16
  }, /*#__PURE__*/React.createElement(FCard, {
    tone: "info"
  }, "Original invoice ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, inv?.id || "—"), " for ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, fEur(max)), " (", inv?.customer || "—", ")."), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Type"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: kind,
    onChange: setKind,
    columns: 2,
    options: [{
      value: "full",
      label: "Full credit",
      hint: `Cancels the entire ${fEur(max)}`
    }, {
      value: "partial",
      label: "Partial credit",
      hint: "Choose an amount"
    }]
  })), kind === "partial" && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Amount (\u20AC)"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement("input", {
    type: "number",
    value: amount / 100,
    onChange: e => setAmount(Math.round(parseFloat(e.target.value || "0") * 100)),
    max: max / 100,
    style: {
      width: "100%",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: "10px 12px",
      font: "500 14px/20px Roboto",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 6
    }
  }), /*#__PURE__*/React.createElement(FHelp, null, "Up to ", fEur(max), ".")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Reason"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: reason,
    onChange: setReason,
    columns: 2,
    options: CREDIT_REASONS.map(r => ({
      value: r.id,
      label: r.label
    }))
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Apply credit as"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FRadio, {
    value: refund,
    onChange: setRefund,
    columns: 2,
    options: [{
      value: "invoice",
      label: "Account credit",
      hint: "Applied to the next invoice"
    }, {
      value: "refund",
      label: "Refund to card",
      hint: `Reverse to ${inv?.method || "Visa •• 4242"}`
    }]
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(FLabel, null, "Internal note (optional)"), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 8
    }
  }), /*#__PURE__*/React.createElement(FTextarea, {
    value: note,
    onChange: setNote,
    placeholder: "Linked ticket, approver, \u2026",
    rows: 2
  }))));
}

// ════════════════════════════════════════════════════════════════════════
// Exports
// ════════════════════════════════════════════════════════════════════════
Object.assign(window, {
  ChangePlanFlow,
  PauseFlow,
  CancelFlow,
  ReactivateFlow,
  NewInvoiceFlow,
  VoidInvoiceFlow,
  CreditNoteFlow
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/billing_flows.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/comms.jsx
try { (() => {
// Tier 3 — Communication: Chat inbox, Support Tickets full, Messaging Channels admin
// Backed by: shared/.../chat/, shared/.../ticket/, shared/.../chat/channels/
// Events surfaced:
//   ChatSessionStarted/Reopened/Closed, MessageSent, AgentJoined/Left,
//   PresenceChanged (ONLINE/AWAY/BUSY/OFFLINE), TypingStarted/Stopped,
//   ChatEscalationLinked → CreateTicket
//   TicketCreated/Replied/Closed/Rated, FirstResponseAt, NotificationRequested
//   ChannelConnected/Disconnected (WEB/WHATSAPP/TELEGRAM), QrRefreshed

const {
  useState: useStateC,
  useEffect: useEffectC,
  useRef: useRefC
} = React;

// ─────────────────────────────────────────────────────────────────
// DATA
// ─────────────────────────────────────────────────────────────────

const AGENTS = [{
  id: "priya",
  name: "Priya Shah",
  presence: "ONLINE",
  load: 4,
  cap: 6,
  role: "Senior agent"
}, {
  id: "daan",
  name: "Daan Visser",
  presence: "ONLINE",
  load: 5,
  cap: 5,
  role: "Agent"
}, {
  id: "lina",
  name: "Lina Hartmann",
  presence: "BUSY",
  load: 3,
  cap: 5,
  role: "Agent"
}, {
  id: "marcus",
  name: "Marcus Bekker",
  presence: "AWAY",
  load: 0,
  cap: 5,
  role: "Agent"
}, {
  id: "noor",
  name: "Noor El-Idrissi",
  presence: "OFFLINE",
  load: 0,
  cap: 5,
  role: "Lead"
}];
const ME = "priya";
const CHATS = [{
  id: "chat-9081",
  customer: "Orbit Labs B.V.",
  contactName: "Sanne de Vries",
  channel: "WEB",
  category: "BILLING",
  status: "ACTIVE",
  waitingMs: 0,
  openedAt: "10:42",
  agent: "priya",
  unread: 2,
  lastFromCustomer: true,
  typing: false,
  sla: {
    firstReply: "12s",
    target: "30s",
    breached: false
  },
  transcript: [{
    from: "customer",
    at: "10:42",
    text: "Hoi! Mijn factuur INV-20260418 toont 3× seats maar we zijn maar met 2 op het account, klopt dat?"
  }, {
    from: "agent",
    who: "priya",
    at: "10:42",
    text: "Hi Sanne — even met je meekijken. Eén momentje."
  }, {
    from: "system",
    at: "10:43",
    text: "Sanne is verifying — VAT NL856220135B01 matched on file."
  }, {
    from: "agent",
    who: "priya",
    at: "10:44",
    text: "Ik zie hier dat seat #3 op 12 april is toegevoegd door bram@orbit.io en daarna nooit weer ingetrokken. Wil je 'm nu intrekken? Dan crediteren we automatisch de pro-rated dagen."
  }, {
    from: "customer",
    at: "10:46",
    text: "Ja graag. Hoeveel scheelt dat?"
  }, {
    from: "customer",
    at: "10:46",
    text: "Ook nieuwsgierig of dit op de volgende factuur al doorkomt 🙏"
  }]
}, {
  id: "chat-9080",
  customer: "Northwind GmbH",
  contactName: "Annika Bauer",
  channel: "WHATSAPP",
  category: "TECHNICAL",
  status: "ACTIVE",
  waitingMs: 95000,
  openedAt: "10:38",
  agent: null,
  unread: 1,
  lastFromCustomer: true,
  typing: false,
  sla: {
    firstReply: "—",
    target: "30s",
    breached: true
  },
  transcript: [{
    from: "customer",
    at: "10:38",
    text: "SSO redirect schlägt seit ~30 Minuten fehl. error: redirect_uri_mismatch. Tenant: northwind-eu1."
  }, {
    from: "system",
    at: "10:38",
    text: "Triage: matched topic SSO. Suggested category: TECHNICAL. Confidence 0.87."
  }, {
    from: "customer",
    at: "10:39",
    text: "Live demo um 11:00 — kann das vorher behoben werden?"
  }]
}, {
  id: "chat-9079",
  customer: "Brouwerij De Maan",
  contactName: "Wouter Jansen",
  channel: "TELEGRAM",
  category: "GENERAL",
  status: "ACTIVE",
  waitingMs: 22000,
  openedAt: "10:36",
  agent: "daan",
  unread: 0,
  lastFromCustomer: false,
  typing: true,
  sla: {
    firstReply: "8s",
    target: "30s",
    breached: false
  },
  transcript: [{
    from: "customer",
    at: "10:36",
    text: "Vraag: kunnen we facturen ook per UBL/PEPPOL ontvangen?"
  }, {
    from: "agent",
    who: "daan",
    at: "10:36",
    text: "Ja, dat kan! Even kijken of jullie PEPPOL ID al op het account staat."
  }]
}, {
  id: "chat-9078",
  customer: "Studio Halve Maan",
  contactName: "Fenna Boersma",
  channel: "WEB",
  category: "ACCOUNT",
  status: "ACTIVE",
  waitingMs: 0,
  openedAt: "10:31",
  agent: "lina",
  unread: 0,
  lastFromCustomer: false,
  typing: false,
  sla: {
    firstReply: "5s",
    target: "30s",
    breached: false
  },
  transcript: []
}, {
  id: "chat-9077",
  customer: "Helder & Co.",
  contactName: "Ravi Singh",
  channel: "WEB",
  category: "BILLING",
  status: "QUEUED",
  waitingMs: 14000,
  openedAt: "10:34",
  agent: null,
  unread: 0,
  lastFromCustomer: true,
  typing: false,
  sla: {
    firstReply: "—",
    target: "30s",
    breached: false
  },
  transcript: [{
    from: "customer",
    at: "10:34",
    text: "Iemand beschikbaar over een credit note?"
  }]
}, {
  id: "chat-9076",
  customer: "Bramley & Sons Ltd",
  contactName: "Imogen Bramley",
  channel: "WHATSAPP",
  category: "TECHNICAL",
  status: "QUEUED",
  waitingMs: 38000,
  openedAt: "10:32",
  agent: null,
  unread: 0,
  lastFromCustomer: true,
  typing: false,
  sla: {
    firstReply: "—",
    target: "30s",
    breached: true
  },
  transcript: [{
    from: "customer",
    at: "10:32",
    text: "API returns 502 on /v1/invoices since ~10 min"
  }]
}, {
  id: "chat-9075",
  customer: "Aurora Mediahuis",
  contactName: "Roos Vermeer",
  channel: "WEB",
  category: "ACCOUNT",
  status: "RESOLVED",
  waitingMs: 0,
  openedAt: "10:11",
  agent: "priya",
  closedAt: "10:24",
  unread: 0,
  lastFromCustomer: false,
  typing: false,
  sla: {
    firstReply: "11s",
    target: "30s",
    breached: false,
    csat: 5
  },
  transcript: []
}];
const CANNED = [{
  id: "billing-cycle",
  title: "Billing cycle explained",
  body: "Je facturatieperiode loopt van de 1e tot de laatste van de maand. Pro-rated wijzigingen verschijnen op de eerstvolgende factuur. Laat me weten als ik even mee zal kijken."
}, {
  id: "sso-redirect",
  title: "SSO redirect_uri_mismatch",
  body: "Voor de redirect_uri error: voeg https://app.incedo.io/auth/callback toe aan je IdP en wacht 60s tot de cache refresht. Werkt dat aan jouw kant?"
}, {
  id: "credit-note",
  title: "Credit note timing",
  body: "Credit notes worden binnen 1 werkdag uitgegeven en verschijnen op de volgende factuur als negatief saldo."
}, {
  id: "peppol-setup",
  title: "PEPPOL onboarding",
  body: "Stuur me je PEPPOL Participant ID (0106:xxxxxxx of 9930:xxxx) en jullie schema; we activeren UBL-bezorging in dezelfde dag."
}];
const TICKETS = [{
  id: "T-20260427-0034",
  subject: "API rate limits unclear on /v1/invoices/bulk",
  customer: "Northwind GmbH",
  category: "TECHNICAL",
  priority: "HIGH",
  status: "OPEN",
  assignee: "daan",
  createdAt: "Apr 27 09:14",
  firstResponseAt: "09:21",
  sla: {
    target: "1h",
    remaining: "47m",
    breached: false
  },
  replies: 4,
  lastUpdate: "23m"
}, {
  id: "T-20260427-0033",
  subject: "Cancellation flow refunds wrong amount when paused",
  customer: "Orbit Labs B.V.",
  category: "BILLING",
  priority: "HIGH",
  status: "OPEN",
  assignee: "priya",
  createdAt: "Apr 27 08:52",
  firstResponseAt: "08:58",
  sla: {
    target: "1h",
    remaining: "−12m",
    breached: true
  },
  replies: 8,
  lastUpdate: "11m"
}, {
  id: "T-20260427-0032",
  subject: "Two-factor SMS not arriving on +44 numbers",
  customer: "Bramley & Sons Ltd",
  category: "TECHNICAL",
  priority: "MEDIUM",
  status: "PENDING_CUSTOMER",
  assignee: "lina",
  createdAt: "Apr 26 16:40",
  firstResponseAt: "Apr 26 16:43",
  sla: {
    target: "4h",
    remaining: "2h 18m",
    breached: false
  },
  replies: 3,
  lastUpdate: "1h"
}, {
  id: "T-20260427-0031",
  subject: "PEPPOL endpoint setup walkthrough",
  customer: "Brouwerij De Maan",
  category: "GENERAL",
  priority: "LOW",
  status: "OPEN",
  assignee: null,
  createdAt: "Apr 27 10:02",
  firstResponseAt: null,
  sla: {
    target: "8h",
    remaining: "7h 41m",
    breached: false
  },
  replies: 0,
  lastUpdate: "1h"
}, {
  id: "T-20260427-0030",
  subject: "Need DPA signed by April 30",
  customer: "Aurora Mediahuis",
  category: "ACCOUNT",
  priority: "MEDIUM",
  status: "PENDING_INTERNAL",
  assignee: "noor",
  createdAt: "Apr 26 14:11",
  firstResponseAt: "Apr 26 14:14",
  sla: {
    target: "4h",
    remaining: "3h 02m",
    breached: false
  },
  replies: 5,
  lastUpdate: "2h"
}, {
  id: "T-20260427-0029",
  subject: "Export: please add Polish (pl-PL) locale to invoices",
  customer: "Studio Halve Maan",
  category: "GENERAL",
  priority: "LOW",
  status: "RESOLVED",
  assignee: "priya",
  createdAt: "Apr 24 11:20",
  firstResponseAt: "Apr 24 11:25",
  sla: {
    target: "8h",
    remaining: "—",
    breached: false,
    csat: 5
  },
  replies: 6,
  lastUpdate: "Apr 25"
}, {
  id: "T-20260427-0028",
  subject: "Single sign-on with Entra ID — group sync delay",
  customer: "Helder & Co.",
  category: "TECHNICAL",
  priority: "HIGH",
  status: "RESOLVED",
  assignee: "daan",
  createdAt: "Apr 23 09:00",
  firstResponseAt: "Apr 23 09:05",
  sla: {
    target: "1h",
    remaining: "—",
    breached: false,
    csat: 4
  },
  replies: 11,
  lastUpdate: "Apr 24"
}, {
  id: "T-20260427-0027",
  subject: "Refund issued twice on dispute case 4421",
  customer: "Northwind GmbH",
  category: "BILLING",
  priority: "HIGH",
  status: "CLOSED",
  assignee: "noor",
  createdAt: "Apr 18 13:20",
  firstResponseAt: "Apr 18 13:24",
  sla: {
    target: "1h",
    remaining: "—",
    breached: false,
    csat: 5
  },
  replies: 9,
  lastUpdate: "Apr 22"
}];
const CHANNELS = [{
  id: "web",
  kind: "WEB",
  label: "Web widget",
  status: "CONNECTED",
  connectedAt: "—",
  uptime: "100% (30d)",
  msgs24h: 412,
  queue24h: 14,
  breach24h: 1,
  note: "Embedded on app.incedo.io. SSE transport. Last-Event-ID reconnect via event store.",
  config: [{
    k: "Endpoint",
    v: "wss://app.incedo.io/api/v1/chat"
  }, {
    k: "Origin allow-list",
    v: "*.incedo.io, app.incedo.test"
  }, {
    k: "Hours",
    v: "Mon–Fri 08:00–18:00 CET"
  }]
}, {
  id: "wa-1",
  kind: "WHATSAPP",
  label: "WhatsApp Business — +31 20 808 24 18",
  status: "CONNECTED",
  connectedAt: "Apr 02, 09:14",
  uptime: "99.7% (30d)",
  msgs24h: 168,
  queue24h: 6,
  breach24h: 0,
  note: "Evolution API · session ID: ev_orbit_main · QR rotation every 14d",
  config: [{
    k: "Provider",
    v: "Evolution API (self-hosted)"
  }, {
    k: "Adapter",
    v: "EvolutionApiAdapter"
  }, {
    k: "Webhook",
    v: "POST /api/v1/webhooks/whatsapp"
  }, {
    k: "QR last refresh",
    v: "Apr 24, 11:02"
  }, {
    k: "24h reopen window",
    v: "Enabled"
  }],
  qr: true
}, {
  id: "tg-1",
  kind: "TELEGRAM",
  label: "Telegram bot — @incedo_support",
  status: "CONNECTED",
  connectedAt: "Mar 14, 13:48",
  uptime: "100% (30d)",
  msgs24h: 31,
  queue24h: 0,
  breach24h: 0,
  note: "TelegramBotAdapter · long-poll mode · admin chat id linked",
  config: [{
    k: "Provider",
    v: "Telegram Bot API"
  }, {
    k: "Adapter",
    v: "TelegramBotAdapter"
  }, {
    k: "Webhook",
    v: "POST /api/v1/webhooks/telegram"
  }, {
    k: "Bot token",
    v: "•••••••••••••••• ZyN3"
  }]
}, {
  id: "wa-2",
  kind: "WHATSAPP",
  label: "WhatsApp Business — +49 30 555 02 90",
  status: "DISCONNECTED",
  connectedAt: "—",
  uptime: "—",
  msgs24h: 0,
  queue24h: 0,
  breach24h: 0,
  note: "Session ev_dach_dach lost connection. Scan a fresh QR to reconnect.",
  config: [{
    k: "Provider",
    v: "Evolution API (self-hosted)"
  }, {
    k: "Last seen",
    v: "Apr 26, 18:02"
  }, {
    k: "Last error",
    v: "session.terminated (peer scan removed)"
  }],
  qr: true
}];

// ─────────────────────────────────────────────────────────────────
// SHARED BITS
// ─────────────────────────────────────────────────────────────────

function ChannelDot({
  kind,
  size = 18
}) {
  const map = {
    WEB: {
      bg: "rgba(26,115,232,.12)",
      fg: "#1A73E8",
      g: "◧"
    },
    WHATSAPP: {
      bg: "rgba(0,184,148,.16)",
      fg: "#00B894",
      g: "✆"
    },
    TELEGRAM: {
      bg: "rgba(14,165,233,.16)",
      fg: "#0EA5E9",
      g: "✈"
    }
  };
  const c = map[kind] || map.WEB;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: 5,
      background: c.bg,
      color: c.fg,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      fontSize: Math.round(size * 0.7),
      flexShrink: 0
    }
  }, c.g);
}
function PresenceDot({
  presence,
  size = 8,
  ring
}) {
  const map = {
    ONLINE: "var(--sp-accent-mint)",
    AWAY: "#F4B400",
    BUSY: "var(--sp-accent-warm)",
    OFFLINE: "var(--sp-text-subtle)"
  };
  return /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: "50%",
      background: map[presence] || map.OFFLINE,
      flexShrink: 0,
      boxShadow: ring ? "0 0 0 2px var(--sp-surface)" : "none",
      display: "inline-block"
    }
  });
}
function CategoryBadge({
  category
}) {
  const map = {
    BILLING: {
      variant: "info",
      label: "Billing"
    },
    TECHNICAL: {
      variant: "plum",
      label: "Technical"
    },
    GENERAL: {
      variant: "muted",
      label: "General"
    },
    ACCOUNT: {
      variant: "amber",
      label: "Account"
    }
  };
  const c = map[category] || map.GENERAL;
  return /*#__PURE__*/React.createElement(PBadge, {
    variant: c.variant,
    dot: true
  }, c.label);
}
function PriorityPill({
  priority
}) {
  const map = {
    HIGH: {
      bg: "rgba(217,48,37,.12)",
      fg: "#D93025"
    },
    MEDIUM: {
      bg: "rgba(244,180,0,.18)",
      fg: "#B06000"
    },
    LOW: {
      bg: "var(--sp-surface-2)",
      fg: "var(--sp-text-muted)"
    }
  };
  const c = map[priority] || map.LOW;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 4,
      padding: "2px 8px",
      borderRadius: 4,
      background: c.bg,
      color: c.fg,
      font: "600 10px/14px Roboto",
      textTransform: "uppercase",
      letterSpacing: "0.05em"
    }
  }, priority.charAt(0), /*#__PURE__*/React.createElement("span", {
    style: {
      letterSpacing: "0.06em"
    }
  }, priority.slice(1).toLowerCase()));
}
const fmtElapsed = ms => {
  const s = Math.floor(ms / 1000);
  if (s < 60) return `${s}s`;
  const m = Math.floor(s / 60);
  const r = s % 60;
  return r === 0 ? `${m}m` : `${m}m ${r}s`;
};

// ─────────────────────────────────────────────────────────────────
// CHAT INBOX
// ─────────────────────────────────────────────────────────────────

function ChatInbox({
  state,
  onEscalate
}) {
  const [chats, setChats] = useStateC(CHATS);
  const [activeId, setActiveId] = useStateC(CHATS[0].id);
  const [filter, setFilter] = useStateC("mine");
  const [draft, setDraft] = useStateC("");
  const [showCanned, setShowCanned] = useStateC(false);
  const [presence, setPresence] = useStateC("ONLINE");

  // tick the queued waiting timers
  useEffectC(() => {
    const id = setInterval(() => {
      setChats(cs => cs.map(c => c.status === "QUEUED" ? {
        ...c,
        waitingMs: c.waitingMs + 1000
      } : c));
    }, 1000);
    return () => clearInterval(id);
  }, []);
  const empty = state === "empty";
  const loading = state === "loading";
  const error = state === "error";
  const tabs = [{
    value: "mine",
    label: `Mine (${chats.filter(c => c.agent === ME && c.status === "ACTIVE").length})`
  }, {
    value: "queue",
    label: `Queue (${chats.filter(c => c.status === "QUEUED").length})`
  }, {
    value: "all",
    label: "All active"
  }, {
    value: "resolved",
    label: "Resolved"
  }];
  const visible = empty ? [] : chats.filter(c => {
    if (filter === "mine") return c.agent === ME && c.status === "ACTIVE";
    if (filter === "queue") return c.status === "QUEUED";
    if (filter === "all") return c.status === "ACTIVE" || c.status === "QUEUED";
    if (filter === "resolved") return c.status === "RESOLVED";
    return true;
  });
  const active = chats.find(c => c.id === activeId);
  const claim = chat => {
    setChats(cs => cs.map(c => c.id === chat.id ? {
      ...c,
      agent: ME,
      status: "ACTIVE",
      waitingMs: 0
    } : c));
    setFilter("mine");
    setActiveId(chat.id);
  };
  const send = () => {
    if (!draft.trim() || !active) return;
    const msg = {
      from: "agent",
      who: ME,
      at: new Date().toLocaleTimeString("en-GB", {
        hour: "2-digit",
        minute: "2-digit"
      }),
      text: draft
    };
    setChats(cs => cs.map(c => c.id === active.id ? {
      ...c,
      transcript: [...c.transcript, msg],
      lastFromCustomer: false,
      unread: 0
    } : c));
    setDraft("");
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "320px 1fr 280px",
      height: "100%",
      minHeight: 0,
      gap: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      borderRight: "1px solid var(--sp-border)",
      display: "flex",
      flexDirection: "column",
      minHeight: 0,
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 16px 10px",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.01em"
    }
  }, "Chat"), /*#__PURE__*/React.createElement(PresenceMenu, {
    value: presence,
    onChange: setPresence
  })), /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: filter,
    onChange: setFilter,
    options: tabs
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: "auto"
    }
  }, loading && /*#__PURE__*/React.createElement(ChatListSkeleton, null), error && /*#__PURE__*/React.createElement(ListError, null), empty && /*#__PURE__*/React.createElement(ListEmpty, {
    filter: filter
  }), !loading && !error && visible.map(c => /*#__PURE__*/React.createElement(ChatRow, {
    key: c.id,
    chat: c,
    active: c.id === activeId,
    onClick: () => setActiveId(c.id),
    onClaim: () => claim(c)
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      minHeight: 0,
      minWidth: 0
    }
  }, !active || empty || loading || error ? /*#__PURE__*/React.createElement(CenterEmpty, {
    state: state
  }) : /*#__PURE__*/React.createElement(ChatTranscript, {
    chat: active,
    draft: draft,
    onDraft: setDraft,
    onSend: send,
    showCanned: showCanned,
    onToggleCanned: () => setShowCanned(v => !v),
    onEscalate: () => onEscalate?.(active),
    onClaim: () => claim(active)
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      borderLeft: "1px solid var(--sp-border)",
      overflow: "auto",
      background: "var(--sp-surface)"
    }
  }, active ? /*#__PURE__*/React.createElement(ChatContextPane, {
    chat: active,
    onEscalate: () => onEscalate?.(active)
  }) : null));
}
function PresenceMenu({
  value,
  onChange
}) {
  const [open, setOpen] = useStateC(false);
  const opts = ["ONLINE", "AWAY", "BUSY", "OFFLINE"];
  return /*#__PURE__*/React.createElement("span", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: () => setOpen(v => !v),
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      padding: "5px 9px",
      borderRadius: 6,
      cursor: "pointer",
      background: "var(--sp-surface-2)",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement(PresenceDot, {
    presence: value
  }), value.charAt(0) + value.slice(1).toLowerCase(), /*#__PURE__*/React.createElement(Ico, {
    g: "\u25BE",
    size: 10,
    style: {
      color: "var(--sp-text-subtle)"
    }
  })), open && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "calc(100% + 4px)",
      right: 0,
      zIndex: 30,
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      boxShadow: "var(--sp-shadow-2)",
      minWidth: 140,
      padding: 4
    }
  }, opts.map(o => /*#__PURE__*/React.createElement("div", {
    key: o,
    onClick: () => {
      onChange(o);
      setOpen(false);
    },
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      padding: "8px 10px",
      borderRadius: 6,
      cursor: "pointer",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: o === value ? "var(--sp-surface-2)" : "transparent"
    }
  }, /*#__PURE__*/React.createElement(PresenceDot, {
    presence: o
  }), o.charAt(0) + o.slice(1).toLowerCase()))));
}
function ChatRow({
  chat,
  active,
  onClick,
  onClaim
}) {
  const last = chat.transcript[chat.transcript.length - 1];
  const lastText = last ? last.from === "system" ? `· ${last.text}` : last.text : "—";
  const lastWho = last && last.from === "agent" ? "You: " : "";
  return /*#__PURE__*/React.createElement("div", {
    onClick: onClick,
    style: {
      padding: "12px 14px",
      cursor: "pointer",
      borderLeft: active ? "3px solid #1A73E8" : "3px solid transparent",
      background: active ? "var(--sp-surface-2)" : "transparent",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      flexDirection: "column",
      gap: 5
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: chat.contactName,
    size: 28
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/16px Roboto",
      color: "var(--sp-text)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, chat.contactName), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      whiteSpace: "nowrap"
    }
  }, chat.openedAt)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 6,
      marginTop: 2
    }
  }, /*#__PURE__*/React.createElement(ChannelDot, {
    kind: chat.channel,
    size: 14
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap",
      minWidth: 0,
      flex: 1
    }
  }, chat.customer)))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: chat.unread > 0 ? "500 12px/17px Roboto" : "400 12px/17px Roboto",
      color: chat.unread > 0 ? "var(--sp-text)" : "var(--sp-text-muted)",
      display: "-webkit-box",
      WebkitLineClamp: 2,
      WebkitBoxOrient: "vertical",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, lastWho, lastText), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 6,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(CategoryBadge, {
    category: chat.category
  }), chat.status === "QUEUED" && /*#__PURE__*/React.createElement(PBadge, {
    variant: chat.sla.breached ? "error" : "warm",
    dot: true
  }, "Queued \xB7 ", fmtElapsed(chat.waitingMs)), chat.typing && /*#__PURE__*/React.createElement(PBadge, {
    variant: "info"
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      gap: 2,
      marginRight: 2
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot"
  }), /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot",
    style: {
      animationDelay: "0.15s"
    }
  }), /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot",
    style: {
      animationDelay: "0.3s"
    }
  })), "typing"), chat.unread > 0 && /*#__PURE__*/React.createElement("span", {
    style: {
      marginLeft: "auto",
      minWidth: 20,
      height: 20,
      padding: "0 6px",
      borderRadius: 10,
      background: "#1A73E8",
      color: "#fff",
      font: "600 11px/20px Roboto",
      textAlign: "center"
    }
  }, chat.unread), chat.status === "QUEUED" && /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    style: {
      marginLeft: chat.unread > 0 ? 0 : "auto"
    },
    onClick: e => {
      e.stopPropagation();
      onClaim();
    }
  }, "Claim")));
}
function ChatTranscript({
  chat,
  draft,
  onDraft,
  onSend,
  showCanned,
  onToggleCanned,
  onEscalate,
  onClaim
}) {
  const ref = useRefC(null);
  useEffectC(() => {
    if (ref.current) ref.current.scrollTop = ref.current.scrollHeight;
  }, [chat.id, chat.transcript.length]);
  const onKey = e => {
    if ((e.metaKey || e.ctrlKey) && e.key === "Enter") {
      e.preventDefault();
      onSend();
    }
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      height: "100%",
      minHeight: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "14px 20px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 12,
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement(ChannelDot, {
    kind: chat.channel,
    size: 26
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, chat.contactName, " \xB7 ", chat.customer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      display: "flex",
      alignItems: "center",
      gap: 8,
      marginTop: 2
    }
  }, /*#__PURE__*/React.createElement("span", null, chat.id), /*#__PURE__*/React.createElement("span", {
    style: {
      width: 3,
      height: 3,
      borderRadius: "50%",
      background: "var(--sp-border)"
    }
  }), /*#__PURE__*/React.createElement(CategoryBadge, {
    category: chat.category
  }), chat.sla.breached ? /*#__PURE__*/React.createElement(PBadge, {
    variant: "error",
    dot: true
  }, "SLA breached") : /*#__PURE__*/React.createElement(PBadge, {
    variant: "mint",
    dot: true
  }, "First reply ", chat.sla.firstReply, " \xB7 target ", chat.sla.target))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, chat.status === "QUEUED" && /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    onClick: onClaim
  }, "Claim chat"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2197",
      size: 12
    }),
    onClick: onEscalate
  }, "Escalate to ticket"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2026",
      size: 14
    })
  }))), /*#__PURE__*/React.createElement("div", {
    ref: ref,
    style: {
      flex: 1,
      overflow: "auto",
      padding: "20px 24px",
      background: "var(--sp-canvas)",
      display: "flex",
      flexDirection: "column",
      gap: 12
    }
  }, chat.transcript.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      margin: "auto",
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto",
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 32,
      marginBottom: 8
    }
  }, "\u25CC"), "No messages yet \u2014 say hi.") : chat.transcript.map((m, i) => /*#__PURE__*/React.createElement(Bubble, {
    key: i,
    m: m
  })), chat.typing && /*#__PURE__*/React.createElement(TypingBubble, {
    name: chat.contactName
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--sp-border)",
      background: "var(--sp-surface)"
    }
  }, showCanned && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 16px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      flexWrap: "wrap",
      gap: 6
    }
  }, CANNED.map(c => /*#__PURE__*/React.createElement("span", {
    key: c.id,
    onClick: () => {
      onDraft(c.body);
      onToggleCanned();
    },
    style: {
      padding: "5px 10px",
      borderRadius: 6,
      cursor: "pointer",
      background: "var(--sp-surface-2)",
      color: "var(--sp-text)",
      font: "500 12px/16px Roboto",
      border: "1px solid var(--sp-border)"
    }
  }, c.title))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 16px",
      display: "flex",
      gap: 10,
      alignItems: "flex-end"
    }
  }, /*#__PURE__*/React.createElement("textarea", {
    value: draft,
    onChange: e => onDraft(e.target.value),
    onKeyDown: onKey,
    placeholder: "Reply to customer \xB7 \u2318+Enter to send",
    style: {
      flex: 1,
      minHeight: 60,
      maxHeight: 180,
      resize: "vertical",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: "10px 12px",
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u275D",
      size: 12
    }),
    onClick: onToggleCanned
  }, "Templates"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "md",
    trailing: /*#__PURE__*/React.createElement(Ico, {
      g: "\u27A4",
      size: 12
    }),
    onClick: onSend,
    disabled: !draft.trim()
  }, "Send")))));
}
function Bubble({
  m
}) {
  if (m.from === "system") {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        alignSelf: "center",
        maxWidth: 480,
        padding: "5px 12px",
        borderRadius: 999,
        background: "var(--sp-surface-2)",
        color: "var(--sp-text-muted)",
        font: "400 11px/16px Roboto",
        textAlign: "center"
      }
    }, m.text);
  }
  const mine = m.from === "agent";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignSelf: mine ? "flex-end" : "flex-start",
      maxWidth: "72%"
    }
  }, !mine && /*#__PURE__*/React.createElement(PAvatar, {
    name: "\xB7",
    size: 28,
    bg: "var(--sp-text-muted)"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 14px",
      borderRadius: 12,
      background: mine ? "#1A73E8" : "var(--sp-surface)",
      color: mine ? "#fff" : "var(--sp-text)",
      boxShadow: mine ? "none" : "var(--sp-shadow-1)",
      font: "400 14px/20px Roboto",
      whiteSpace: "pre-wrap",
      borderTopLeftRadius: mine ? 12 : 4,
      borderTopRightRadius: mine ? 4 : 12
    }
  }, m.text, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 10px/14px Roboto",
      marginTop: 4,
      color: mine ? "rgba(255,255,255,.7)" : "var(--sp-text-subtle)",
      textAlign: "right"
    }
  }, m.at, mine && " · ✓✓")));
}
function TypingBubble({
  name
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignSelf: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: name,
    size: 28
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 16px",
      borderRadius: 12,
      borderTopLeftRadius: 4,
      background: "var(--sp-surface)",
      boxShadow: "var(--sp-shadow-1)",
      display: "inline-flex",
      alignItems: "center",
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot"
  }), /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot",
    style: {
      animationDelay: "0.15s"
    }
  }), /*#__PURE__*/React.createElement("span", {
    className: "sp-tdot",
    style: {
      animationDelay: "0.3s"
    }
  })));
}
function ChatContextPane({
  chat,
  onEscalate
}) {
  const agent = AGENTS.find(a => a.id === chat.agent);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 18px 24px",
      display: "flex",
      flexDirection: "column",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Customer"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: chat.contactName,
    size: 36
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, chat.contactName), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, chat.customer)))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Session"), /*#__PURE__*/React.createElement(KVRow, {
    k: "Channel",
    v: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(ChannelDot, {
      kind: chat.channel,
      size: 14
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        marginLeft: 6
      }
    }, chat.channel.charAt(0) + chat.channel.slice(1).toLowerCase()))
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "Category",
    v: /*#__PURE__*/React.createElement(CategoryBadge, {
      category: chat.category
    })
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "Started",
    v: chat.openedAt
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "Session ID",
    v: /*#__PURE__*/React.createElement("span", {
      style: {
        fontFamily: "Roboto Mono",
        fontSize: 11,
        color: "var(--sp-text-muted)"
      }
    }, chat.id)
  }), agent && /*#__PURE__*/React.createElement(KVRow, {
    k: "Assigned",
    v: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PresenceDot, {
      presence: agent.presence
    }), " ", /*#__PURE__*/React.createElement("span", {
      style: {
        marginLeft: 6
      }
    }, agent.name))
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "SLA"), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 12,
      borderRadius: 8,
      background: chat.sla.breached ? "rgba(217,48,37,.08)" : "var(--sp-surface-2)",
      border: chat.sla.breached ? "1px solid rgba(217,48,37,.3)" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "First reply"), /*#__PURE__*/React.createElement("span", null, chat.sla.firstReply)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("span", null, "Target"), /*#__PURE__*/React.createElement("span", null, chat.sla.target)), chat.sla.breached && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      font: "500 12px/16px Roboto",
      color: "#D93025"
    }
  }, "\u26A0 SLA breached \u2014 please prioritise."))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Quick actions"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2197",
      size: 12
    }),
    onClick: onEscalate,
    style: {
      justifyContent: "flex-start"
    }
  }, "Escalate to ticket"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u25E7",
      size: 12
    }),
    style: {
      justifyContent: "flex-start"
    }
  }, "Open Customer 360"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u270E",
      size: 12
    }),
    style: {
      justifyContent: "flex-start"
    }
  }, "Add internal note"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2715",
      size: 12
    }),
    style: {
      justifyContent: "flex-start",
      color: "#D93025"
    }
  }, "Close chat"))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Recent activity"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Sub: ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)"
    }
  }, "Growth \xB7 \u20AC189/mo"), /*#__PURE__*/React.createElement("br", null), "Last invoice: ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)"
    }
  }, "INV-20260418 \u2014 Paid"), /*#__PURE__*/React.createElement("br", null), "Open tickets: ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)"
    }
  }, "1"))));
}
function KVRow({
  k,
  v
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      padding: "5px 0",
      font: "400 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, k), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)",
      display: "inline-flex",
      alignItems: "center"
    }
  }, v));
}
function ChatListSkeleton() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 14
    }
  }, [0, 1, 2, 3, 4].map(i => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 28,
    h: 28,
    r: 14
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "70%",
    h: 12
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 4
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "40%",
    h: 10
  }))), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 10
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 4
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "80%",
    h: 10
  }))));
}
function ListEmpty({
  filter
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 32,
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 32,
      marginBottom: 8
    }
  }, "\u25CC"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, filter === "queue" ? "Queue empty" : filter === "mine" ? "No active chats assigned to you" : "No chats"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      marginTop: 4
    }
  }, filter === "queue" ? "Customers waiting will appear here." : "Set yourself ONLINE to start receiving routing."));
}
function ListError() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 32,
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 32,
      marginBottom: 8,
      color: "#D93025"
    }
  }, "\u26A0"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Couldn't load chats"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "SSE connection lost. Trying to reconnect\u2026"));
}
function CenterEmpty({
  state
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      flexDirection: "column",
      gap: 8,
      color: "var(--sp-text-muted)",
      background: "var(--sp-canvas)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 48
    }
  }, "\u25E7"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, state === "loading" ? "Connecting to chat hub…" : state === "error" ? "Connection lost" : state === "empty" ? "Inbox zero" : "Pick a chat to start"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      maxWidth: 360,
      textAlign: "center"
    }
  }, state === "empty" ? "No active conversations. Set yourself ONLINE to start receiving routing." : "All transcripts are kept in the event store and can be replayed via Last-Event-ID."));
}

// ─────────────────────────────────────────────────────────────────
// TICKETS FULL
// ─────────────────────────────────────────────────────────────────

function TicketsFull({
  state
}) {
  const [tab, setTab] = useStateC("open");
  const [activeId, setActiveId] = useStateC(TICKETS[0].id);
  const [search, setSearch] = useStateC("");
  const [category, setCategory] = useStateC("all");
  const [priority, setPriority] = useStateC("all");
  const empty = state === "empty";
  const loading = state === "loading";
  const tabs = [{
    value: "open",
    label: `Open (${TICKETS.filter(t => t.status === "OPEN").length})`
  }, {
    value: "pending",
    label: "Pending"
  }, {
    value: "mine",
    label: `Mine (${TICKETS.filter(t => t.assignee === ME).length})`
  }, {
    value: "all",
    label: "All"
  }, {
    value: "resolved",
    label: "Resolved"
  }];
  const visible = empty ? [] : TICKETS.filter(t => {
    if (tab === "open" && t.status !== "OPEN") return false;
    if (tab === "pending" && !t.status.startsWith("PENDING")) return false;
    if (tab === "mine" && t.assignee !== ME) return false;
    if (tab === "resolved" && t.status !== "RESOLVED" && t.status !== "CLOSED") return false;
    if (category !== "all" && t.category !== category) return false;
    if (priority !== "all" && t.priority !== priority) return false;
    if (search && !(t.subject + t.customer + t.id).toLowerCase().includes(search.toLowerCase())) return false;
    return true;
  });
  const active = TICKETS.find(t => t.id === activeId) || visible[0];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 420px",
      height: "100%",
      minHeight: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      minHeight: 0,
      borderRight: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 24px 12px",
      display: "flex",
      flexDirection: "column",
      gap: 12,
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: tab,
    onChange: setTab,
    options: tabs
  }), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+",
      size: 12
    })
  }, "New ticket")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search ticket id, subject, customer\u2026",
    value: search,
    onChange: setSearch,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2315",
      size: 12
    }),
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(FilterSelect, {
    label: "Category",
    value: category,
    onChange: setCategory,
    options: [{
      value: "all",
      label: "All categories"
    }, {
      value: "BILLING",
      label: "Billing"
    }, {
      value: "TECHNICAL",
      label: "Technical"
    }, {
      value: "GENERAL",
      label: "General"
    }, {
      value: "ACCOUNT",
      label: "Account"
    }]
  }), /*#__PURE__*/React.createElement(FilterSelect, {
    label: "Priority",
    value: priority,
    onChange: setPriority,
    options: [{
      value: "all",
      label: "Any priority"
    }, {
      value: "HIGH",
      label: "High"
    }, {
      value: "MEDIUM",
      label: "Medium"
    }, {
      value: "LOW",
      label: "Low"
    }]
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: "auto"
    }
  }, loading ? /*#__PURE__*/React.createElement(TicketTableSkeleton, null) : empty ? /*#__PURE__*/React.createElement(TicketTableEmpty, null) : /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse",
      font: "400 13px/18px Roboto"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    style: {
      background: "var(--sp-surface-2)"
    }
  }, ["Ticket", "Subject", "Customer", "Category", "Priority", "Assignee", "SLA", "Updated"].map(h => /*#__PURE__*/React.createElement("th", {
    key: h,
    style: {
      textAlign: "left",
      padding: "10px 12px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.04em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, visible.map(t => {
    const assignee = AGENTS.find(a => a.id === t.assignee);
    const isActive = active && t.id === active.id;
    return /*#__PURE__*/React.createElement("tr", {
      key: t.id,
      onClick: () => setActiveId(t.id),
      style: {
        cursor: "pointer",
        background: isActive ? "var(--sp-surface-2)" : "transparent",
        borderLeft: isActive ? "3px solid #1A73E8" : "3px solid transparent"
      }
    }, /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        fontFamily: "Roboto Mono",
        fontSize: 11,
        color: "var(--sp-text-muted)"
      }
    }, t.id)), /*#__PURE__*/React.createElement("td", {
      style: {
        ...tdStyle,
        color: "var(--sp-text)",
        fontWeight: 500,
        maxWidth: 380,
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, t.subject), /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, t.customer), /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, /*#__PURE__*/React.createElement(CategoryBadge, {
      category: t.category
    })), /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, /*#__PURE__*/React.createElement(PriorityPill, {
      priority: t.priority
    })), /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, assignee ? /*#__PURE__*/React.createElement("span", {
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 6
      }
    }, /*#__PURE__*/React.createElement(PAvatar, {
      name: assignee.name,
      size: 20
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text)"
      }
    }, assignee.name.split(" ")[0])) : /*#__PURE__*/React.createElement(PBadge, {
      variant: "muted"
    }, "Unassigned")), /*#__PURE__*/React.createElement("td", {
      style: tdStyle
    }, t.sla.breached ? /*#__PURE__*/React.createElement(PBadge, {
      variant: "error",
      dot: true
    }, t.sla.remaining) : /*#__PURE__*/React.createElement(PBadge, {
      variant: t.sla.remaining.includes("h") ? "muted" : "warm",
      dot: true
    }, t.sla.remaining)), /*#__PURE__*/React.createElement("td", {
      style: {
        ...tdStyle,
        color: "var(--sp-text-muted)"
      }
    }, t.lastUpdate));
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      overflow: "auto",
      background: "var(--sp-surface)"
    }
  }, active && !empty && !loading ? /*#__PURE__*/React.createElement(TicketThread, {
    ticket: active
  }) : /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 32,
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 36,
      marginBottom: 8
    }
  }, "\u2709"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Select a ticket"))));
}
const tdStyle = {
  padding: "12px",
  borderBottom: "1px solid var(--sp-border)",
  verticalAlign: "middle"
};
function FilterSelect({
  label,
  value,
  onChange,
  options
}) {
  return /*#__PURE__*/React.createElement("select", {
    value: value,
    onChange: e => onChange(e.target.value),
    style: {
      padding: "7px 10px",
      borderRadius: 8,
      border: "1px solid var(--sp-border)",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      font: "400 13px/18px Roboto",
      cursor: "pointer",
      outline: "none"
    }
  }, options.map(o => /*#__PURE__*/React.createElement("option", {
    key: o.value,
    value: o.value
  }, o.label)));
}
function TicketTableSkeleton() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16
    }
  }, [0, 1, 2, 3, 4].map(i => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 10,
      marginBottom: 12,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 120,
    h: 12
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "50%",
    h: 12
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 100,
    h: 12
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 80,
    h: 12
  }))));
}
function TicketTableEmpty() {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 56,
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 40,
      marginBottom: 10
    }
  }, "\u2709"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, "Inbox zero"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      marginTop: 4,
      maxWidth: 380,
      margin: "4px auto 0"
    }
  }, "No tickets match your filters. Customers can open one from the portal or via the chat escalation flow."));
}
function TicketThread({
  ticket
}) {
  const assignee = AGENTS.find(a => a.id === ticket.assignee);
  const messages = ticket.replies > 0 ? generateThread(ticket) : [];
  const [draft, setDraft] = useStateC("");
  const [internal, setInternal] = useStateC(false);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      height: "100%",
      minHeight: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 20px",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      letterSpacing: "0.04em",
      textTransform: "uppercase",
      marginBottom: 4,
      fontFamily: "Roboto Mono"
    }
  }, ticket.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 18px/24px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.01em",
      marginBottom: 8
    }
  }, ticket.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexWrap: "wrap",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(CategoryBadge, {
    category: ticket.category
  }), /*#__PURE__*/React.createElement(PriorityPill, {
    priority: ticket.priority
  }), /*#__PURE__*/React.createElement(PBadge, {
    variant: ticket.status === "OPEN" ? "info" : ticket.status === "RESOLVED" || ticket.status === "CLOSED" ? "mint" : "amber",
    dot: true
  }, ticket.status.replace("_", " ")), ticket.sla.breached && /*#__PURE__*/React.createElement(PBadge, {
    variant: "error",
    dot: true
  }, "SLA breach \xB7 ", ticket.sla.remaining), ticket.sla.csat && /*#__PURE__*/React.createElement(PBadge, {
    variant: "mint"
  }, "\u2605 ", ticket.sla.csat, "/5")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 8,
      marginTop: 14,
      font: "400 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement(KVRow, {
    k: "Customer",
    v: /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text)"
      }
    }, ticket.customer)
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "Created",
    v: ticket.createdAt
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "Assignee",
    v: assignee ? /*#__PURE__*/React.createElement("span", {
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 6
      }
    }, /*#__PURE__*/React.createElement(PresenceDot, {
      presence: assignee.presence
    }), " ", assignee.name) : /*#__PURE__*/React.createElement(PBadge, {
      variant: "muted"
    }, "Unassigned")
  }), /*#__PURE__*/React.createElement(KVRow, {
    k: "First reply",
    v: ticket.firstResponseAt || "—"
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: "auto",
      padding: "16px 20px",
      background: "var(--sp-canvas)",
      display: "flex",
      flexDirection: "column",
      gap: 12
    }
  }, messages.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      margin: "auto",
      color: "var(--sp-text-muted)",
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 28,
      marginBottom: 6
    }
  }, "\u25CC"), "No replies yet \u2014 claim and respond within ", ticket.sla.target, ".") : messages.map((m, i) => /*#__PURE__*/React.createElement(TicketReply, {
    key: i,
    m: m
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--sp-border)",
      padding: 16,
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginBottom: 8
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: internal ? "internal" : "public",
    onChange: v => setInternal(v === "internal"),
    options: [{
      value: "public",
      label: "Reply to customer"
    }, {
      value: "internal",
      label: "Internal note"
    }]
  })), /*#__PURE__*/React.createElement("textarea", {
    value: draft,
    onChange: e => setDraft(e.target.value),
    placeholder: internal ? "Visible only to your team…" : "Reply to customer…",
    style: {
      width: "100%",
      minHeight: 80,
      resize: "vertical",
      border: `1px solid ${internal ? "rgba(244,180,0,.6)" : "var(--sp-border)"}`,
      borderRadius: 8,
      padding: "10px 12px",
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: internal ? "rgba(244,180,0,.06)" : "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginTop: 8,
      justifyContent: "flex-end"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u270E",
      size: 12
    })
  }, "Templates"), ticket.status === "OPEN" && /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Resolve"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    disabled: !draft.trim(),
    trailing: /*#__PURE__*/React.createElement(Ico, {
      g: "\u27A4",
      size: 12
    })
  }, internal ? "Add note" : "Send reply"))));
}
function generateThread(ticket) {
  const customer = ticket.customer.split(" ")[0];
  const arr = [{
    from: "customer",
    who: customer,
    at: ticket.createdAt,
    text: `Hi team, ${ticket.subject.toLowerCase()}. Could someone take a look?`
  }, {
    from: "agent",
    who: AGENTS.find(a => a.id === ticket.assignee)?.name || "Agent",
    at: ticket.firstResponseAt || "—",
    text: `Hi ${customer}, we're on it. Sharing context with engineering and circling back within ${ticket.sla.target}.`
  }, {
    from: "internal",
    who: "Daan",
    at: ticket.firstResponseAt || "—",
    text: `Internal: similar to T-…0028 last week. Kicking off retro link with PSP team.`
  }, {
    from: "customer",
    who: customer,
    at: "—",
    text: "Thanks — happy to provide additional logs if helpful."
  }];
  return arr.slice(0, Math.min(ticket.replies, arr.length));
}
function TicketReply({
  m
}) {
  if (m.from === "internal") {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        padding: "10px 14px",
        borderRadius: 8,
        alignSelf: "stretch",
        background: "rgba(244,180,0,.08)",
        border: "1px solid rgba(244,180,0,.3)",
        font: "400 13px/19px Roboto",
        color: "var(--sp-text)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "#B06000",
        textTransform: "uppercase",
        letterSpacing: "0.05em",
        marginBottom: 4
      }
    }, "Internal note \xB7 ", m.who, " \xB7 ", m.at), m.text);
  }
  const mine = m.from === "agent";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignSelf: mine ? "flex-end" : "flex-start",
      maxWidth: "88%"
    }
  }, !mine && /*#__PURE__*/React.createElement(PAvatar, {
    name: m.who,
    size: 28
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 3,
      textAlign: mine ? "right" : "left"
    }
  }, m.who, " \xB7 ", m.at), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 14px",
      borderRadius: 12,
      background: mine ? "#1A73E8" : "var(--sp-surface)",
      color: mine ? "#fff" : "var(--sp-text)",
      boxShadow: mine ? "none" : "var(--sp-shadow-1)",
      font: "400 14px/20px Roboto",
      borderTopLeftRadius: mine ? 12 : 4,
      borderTopRightRadius: mine ? 4 : 12
    }
  }, m.text)));
}

// ─────────────────────────────────────────────────────────────────
// CHANNELS ADMIN
// ─────────────────────────────────────────────────────────────────

function ChannelsAdmin({
  state,
  onShowQr
}) {
  const empty = state === "empty";
  const loading = state === "loading";
  const totals = {
    msgs: CHANNELS.reduce((s, c) => s + c.msgs24h, 0),
    breaches: CHANNELS.reduce((s, c) => s + c.breach24h, 0),
    connected: CHANNELS.filter(c => c.status === "CONNECTED").length,
    total: CHANNELS.length
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      display: "flex",
      flexDirection: "column",
      gap: 20,
      maxWidth: 1280
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 22px/28px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Messaging channels"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Inbound transports for the chat hub. WhatsApp via Evolution API, Telegram via Bot API.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "md",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+",
      size: 12
    })
  }, "Add channel")), loading ? /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(380px, 1fr))",
      gap: 16
    }
  }, [0, 1, 2].map(i => /*#__PURE__*/React.createElement(PSkeleton, {
    key: i,
    h: 220,
    r: 12
  }))) : empty ? /*#__PURE__*/React.createElement(PCard, {
    pad: 56
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 44,
      marginBottom: 12
    }
  }, "\u2706"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, "No messaging channels yet"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      maxWidth: 460,
      margin: "6px auto 16px"
    }
  }, "Connect WhatsApp Business or Telegram to receive customer messages alongside the web widget."), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+",
      size: 12
    })
  }, "Add your first channel"))) : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(StatCard, {
    label: "Channels online",
    value: `${totals.connected} / ${totals.total}`,
    hint: "real-time"
  }), /*#__PURE__*/React.createElement(StatCard, {
    label: "Inbound (24h)",
    value: totals.msgs.toLocaleString(),
    hint: "customer \u2192 agent"
  }), /*#__PURE__*/React.createElement(StatCard, {
    label: "Queued now",
    value: CHANNELS.reduce((s, c) => s + c.queue24h, 0).toString(),
    hint: "across all channels"
  }), /*#__PURE__*/React.createElement(StatCard, {
    label: "SLA breaches (24h)",
    value: totals.breaches.toString(),
    tone: totals.breaches > 0 ? "error" : "muted",
    hint: "first-reply > target"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(420px, 1fr))",
      gap: 16
    }
  }, CHANNELS.map(c => /*#__PURE__*/React.createElement(ChannelCard, {
    key: c.id,
    channel: c,
    onShowQr: onShowQr
  })))));
}
function StatCard({
  label,
  value,
  hint,
  tone = "default"
}) {
  return /*#__PURE__*/React.createElement(PCard, {
    pad: 16
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 26px/32px Roboto",
      color: tone === "error" && value !== "0" ? "#D93025" : "var(--sp-text)",
      letterSpacing: "-0.02em",
      marginTop: 4
    }
  }, value), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 2
    }
  }, hint));
}
function ChannelCard({
  channel,
  onShowQr
}) {
  const connected = channel.status === "CONNECTED";
  return /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    hover: true
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 18px",
      display: "flex",
      alignItems: "flex-start",
      gap: 12,
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(ChannelDot, {
    kind: channel.kind,
    size: 36
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/19px Roboto",
      color: "var(--sp-text)"
    }
  }, channel.label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, channel.note)), /*#__PURE__*/React.createElement(PBadge, {
    variant: connected ? "mint" : "error",
    dot: true
  }, connected ? "Connected" : "Disconnected")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 18px",
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: 12,
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(Stat, {
    compact: true,
    label: "Inbound 24h",
    value: channel.msgs24h
  }), /*#__PURE__*/React.createElement(Stat, {
    compact: true,
    label: "Uptime",
    value: channel.uptime
  }), /*#__PURE__*/React.createElement(Stat, {
    compact: true,
    label: "Connected",
    value: channel.connectedAt
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 18px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Configuration"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 4
    }
  }, channel.config.map((kv, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      justifyContent: "space-between",
      gap: 16,
      font: "400 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, kv.k), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)",
      textAlign: "right",
      fontFamily: kv.v.includes("/") || kv.v.includes("•") ? "Roboto Mono" : "Roboto",
      fontSize: kv.v.includes("•") ? 11 : 12
    }
  }, kv.v))))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 18px 16px",
      display: "flex",
      gap: 8,
      borderTop: "1px solid var(--sp-border)"
    }
  }, channel.qr && /*#__PURE__*/React.createElement(PButton, {
    variant: connected ? "secondary" : "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u25A6",
      size: 12
    }),
    onClick: () => onShowQr?.(channel)
  }, connected ? "Refresh QR" : "Scan to connect"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2699",
      size: 12
    })
  }, "Settings"), connected && /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u23FB",
      size: 12
    }),
    style: {
      color: "#D93025",
      marginLeft: "auto"
    }
  }, "Disconnect")));
}
function Stat({
  label,
  value,
  compact
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 10px/13px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: `600 ${compact ? 14 : 16}px/20px Roboto`,
      color: "var(--sp-text)",
      marginTop: 2
    }
  }, value));
}

// QR modal content (rendered via <PModal/>)
function QrModalBody({
  channel
}) {
  if (!channel) return null;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      textAlign: "center",
      maxWidth: 380
    }
  }, "Open WhatsApp \u2192 Settings \u2192 Linked devices \u2192 ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, "Link a device"), ", then scan this code."), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      padding: 16,
      background: "var(--sp-surface-2)",
      borderRadius: 12,
      border: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(FakeQr, null), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "50%",
      left: "50%",
      transform: "translate(-50%, -50%)",
      width: 48,
      height: 48,
      borderRadius: 8,
      background: "#fff",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      boxShadow: "0 2px 8px rgba(0,0,0,.2)"
    }
  }, /*#__PURE__*/React.createElement(ChannelDot, {
    kind: "WHATSAPP",
    size: 32
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "QR rotates in ", /*#__PURE__*/React.createElement("strong", {
    style: {
      color: "var(--sp-text)"
    }
  }, "14d 06h"), "."));
}
function FakeQr() {
  // Deterministic pseudo-QR — 25×25 grid of squares
  const cells = [];
  let seed = 1337;
  const rnd = () => {
    seed = (seed * 9301 + 49297) % 233280;
    return seed / 233280;
  };
  const N = 25;
  for (let y = 0; y < N; y++) {
    for (let x = 0; x < N; x++) {
      // finder squares at corners
      const corner = x < 7 && y < 7 || x > N - 8 && y < 7 || x < 7 && y > N - 8;
      const inner = corner && (x === 0 || x === 6 || x > N - 8 && (x === N - 1 || x === N - 7) || y === 0 || y === 6 || y > N - 8 && (y === N - 1 || y === N - 7));
      let on = corner ? !(x > 1 && x < 5 && y > 1 && y < 5 || x > N - 6 && x < N - 2 && y > 1 && y < 5 || x > 1 && x < 5 && y > N - 6 && y < N - 2) : rnd() > 0.55;
      if (corner && ((x === 2 || x === 3 || x === 4) && (y === 2 || y === 3 || y === 4) || (x === N - 3 || x === N - 4 || x === N - 5) && (y === 2 || y === 3 || y === 4) || (x === 2 || x === 3 || x === 4) && (y === N - 3 || y === N - 4 || y === N - 5))) on = true;
      cells.push(/*#__PURE__*/React.createElement("rect", {
        key: `${x}-${y}`,
        x: x * 8,
        y: y * 8,
        width: 7,
        height: 7,
        rx: 1,
        fill: on ? "var(--sp-text)" : "transparent"
      }));
    }
  }
  return /*#__PURE__*/React.createElement("svg", {
    width: 224,
    height: 224,
    viewBox: `0 0 ${N * 8} ${N * 8}`
  }, cells);
}

// ─────────────────────────────────────────────────────────────────
// Export to global window
// ─────────────────────────────────────────────────────────────────

Object.assign(window, {
  ChatInbox,
  TicketsFull,
  ChannelsAdmin,
  QrModalBody,
  AGENTS,
  CHATS,
  TICKETS,
  CHANNELS,
  CANNED,
  ChannelDot,
  PresenceDot,
  CategoryBadge,
  PriorityPill
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/comms.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/complaints.jsx
try { (() => {
// ─────────────────────────────────────────────────────────────────────────
// Complaints module — portal (customer-facing) + admin (case-handler) views.
// Backed by specs/domain/complaints.md — lifecycle, SLA matrix, root-cause.
//
// Key rule from the spec: **a complaint can be filed without login**.
// The anonymous intake form lives in complaint.html; this file provides
// the form component + the authenticated portal/admin surfaces.
// ─────────────────────────────────────────────────────────────────────────

// ─── Taxonomies from the spec ──────────────────────────────────────────
const CX_CATEGORIES = [{
  k: "BILLING",
  label: "Billing & invoices"
}, {
  k: "PRODUCT",
  label: "Product issue"
}, {
  k: "SERVICE",
  label: "Service quality"
}, {
  k: "DELIVERY",
  label: "Delivery / onboarding"
}, {
  k: "PRIVACY",
  label: "Privacy & data"
}, {
  k: "STAFF_CONDUCT",
  label: "Staff conduct"
}, {
  k: "OTHER",
  label: "Something else"
}];
const CX_PRIORITIES = [{
  k: "URGENT",
  label: "Urgent",
  color: "#D93025",
  frt: "1h",
  res: "8h"
}, {
  k: "HIGH",
  label: "High",
  color: "#E8710A",
  frt: "4h",
  res: "24h"
}, {
  k: "MEDIUM",
  label: "Medium",
  color: "#F9A825",
  frt: "24h",
  res: "5d"
}, {
  k: "LOW",
  label: "Low",
  color: "#5F6368",
  frt: "48h",
  res: "10d"
}];
const CX_STATUS = {
  RECEIVED: {
    label: "Received",
    tone: "muted",
    step: 1
  },
  REGISTERED: {
    label: "Registered",
    tone: "info",
    step: 2
  },
  UNDER_REVIEW: {
    label: "Under review",
    tone: "info",
    step: 3
  },
  IN_RESOLUTION: {
    label: "In resolution",
    tone: "amber",
    step: 4
  },
  AWAITING_CUSTOMER: {
    label: "Waiting on you",
    tone: "amber",
    step: 4
  },
  RESOLVED: {
    label: "Resolved",
    tone: "mint",
    step: 5
  },
  CLOSED: {
    label: "Closed",
    tone: "muted",
    step: 6
  },
  REJECTED: {
    label: "Rejected",
    tone: "muted",
    step: 6
  }
};
const CX_LIFECYCLE = [{
  k: "RECEIVED",
  label: "Received",
  hint: "We got your complaint"
}, {
  k: "REGISTERED",
  label: "Registered",
  hint: "Logged and categorised"
}, {
  k: "UNDER_REVIEW",
  label: "Assessing",
  hint: "Priority and severity set"
}, {
  k: "IN_RESOLUTION",
  label: "Resolving",
  hint: "A case-handler is working on it"
}, {
  k: "RESOLVED",
  label: "Resolved",
  hint: "Outcome communicated"
}, {
  k: "CLOSED",
  label: "Closed",
  hint: "Root cause logged"
}];
const CX_CHANNELS = [{
  k: "IN_APP",
  label: "Customer portal",
  icon: "◈"
}, {
  k: "EMAIL",
  label: "Email",
  icon: "✉"
}, {
  k: "PHONE",
  label: "Phone",
  icon: "☏"
}, {
  k: "CHAT",
  label: "Live chat",
  icon: "◌"
}, {
  k: "LETTER",
  label: "Letter",
  icon: "✎"
}, {
  k: "SOCIAL",
  label: "Social media",
  icon: "◐"
}, {
  k: "OTHER",
  label: "Other",
  icon: "•"
}];
const CX_ROOT_CAUSES = [{
  k: "billing.invoice.vat-miscalc",
  label: "Billing · invoice VAT miscalculated"
}, {
  k: "billing.invoice.wrong-amount",
  label: "Billing · invoice wrong amount"
}, {
  k: "billing.payment.failed-charge",
  label: "Billing · payment failed"
}, {
  k: "product.bug.core-flow",
  label: "Product · bug in core flow"
}, {
  k: "product.performance.slow",
  label: "Product · performance"
}, {
  k: "service.response-time",
  label: "Service · slow response time"
}, {
  k: "journey.onboarding.step-3-confusing",
  label: "Journey · confusing onboarding"
}, {
  k: "journey.cancellation.friction",
  label: "Journey · cancellation friction"
}, {
  k: "privacy.data-request.delayed",
  label: "Privacy · DSR delayed"
}, {
  k: "staff.communication-tone",
  label: "Staff · communication tone"
}];

// ─── Mock data ──────────────────────────────────────────────────────────
// My (portal) complaints — scoped to ME.
const MY_COMPLAINTS = [{
  id: "C-20260418-0017",
  subject: "Charged twice for October — duplicate invoice",
  description: "On October 1 I received two invoices for the same amount (€216). My card was charged twice. I'd like the duplicate refunded and want to understand how this happened.",
  status: "IN_RESOLUTION",
  category: "BILLING",
  priority: "HIGH",
  severity: "MODERATE",
  channel: "IN_APP",
  assignee: "Priya Shah",
  assigneeRole: "Senior Case Handler",
  subscriptionId: "SUB-004211",
  invoiceId: "INV-20251002",
  received: "Apr 18, 2026 · 14:22",
  firstReply: "Apr 18, 2026 · 15:41",
  slaFRT: {
    due: "Apr 18, 18:22",
    ok: true,
    hit: "1h 19m"
  },
  slaRes: {
    due: "Apr 19, 14:22",
    ok: true
  },
  compensation: {
    kind: "REFUND",
    amount: 21600,
    note: "Full refund of duplicate charge. Processed via Stripe, 3–5 business days."
  },
  reopenCount: 0,
  anonymous: false
}, {
  id: "C-20260402-0008",
  subject: "Cancellation flow kept me going in circles",
  description: "I tried to cancel my Growth plan 3 times last week. Each time, the portal redirected me back to 'are you sure?' and eventually errored out. I called support and the agent was helpful but this shouldn't be so hard.",
  status: "CLOSED",
  category: "DELIVERY",
  priority: "MEDIUM",
  severity: "MODERATE",
  channel: "PHONE",
  assignee: "Daan Visser",
  assigneeRole: "Case Handler",
  received: "Apr 02, 2026 · 11:05",
  firstReply: "Apr 02, 2026 · 12:40",
  resolved: "Apr 04, 2026 · 16:20",
  closed: "Apr 05, 2026 · 09:12",
  rootCauseCode: "journey.cancellation.friction",
  structuralIssue: true,
  slaFRT: {
    ok: true,
    hit: "1h 35m"
  },
  slaRes: {
    ok: true
  },
  compensation: {
    kind: "APOLOGY",
    amount: null,
    note: "Escalated to product team as a structural UX issue. Cancellation re-designed in v2.4."
  },
  reopenCount: 0,
  csatRating: 4,
  anonymous: false
}];

// Admin queue — broader set across customers.
const ADMIN_COMPLAINTS = [{
  id: "C-20260418-0017",
  subject: "Charged twice for October — duplicate invoice",
  customer: "Orbit Labs B.V.",
  contact: "Elin Karlsson",
  contactEmail: "elin@orbitlabs.io",
  status: "IN_RESOLUTION",
  category: "BILLING",
  priority: "HIGH",
  severity: "MODERATE",
  channel: "IN_APP",
  assignee: "Priya Shah",
  received: "Apr 18, 14:22",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: 420,
  unread: 1,
  anonymous: false
}, {
  id: "C-20260419-0023",
  subject: "SSO broken after your maintenance window",
  customer: "Nordvik Energy",
  contact: "Mikkel Strand",
  contactEmail: "mikkel@nordvik.no",
  status: "UNDER_REVIEW",
  category: "PRODUCT",
  priority: "URGENT",
  severity: "CRITICAL",
  channel: "EMAIL",
  assignee: "Priya Shah",
  received: "Apr 19, 08:04",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: 120,
  unread: 3,
  anonymous: false
}, {
  id: "C-20260419-0025",
  subject: "Rude response from your chat agent",
  customer: "—",
  contact: "Anonymous · alex.r@protonmail.com",
  contactEmail: "alex.r@protonmail.com",
  status: "REGISTERED",
  category: "STAFF_CONDUCT",
  priority: "MEDIUM",
  severity: "MODERATE",
  channel: "IN_APP",
  assignee: "—",
  received: "Apr 19, 10:47",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: 7200,
  unread: 1,
  anonymous: true
}, {
  id: "C-20260417-0011",
  subject: "Third invoice with wrong VAT for Belgian entity",
  customer: "Orbit Labs B.V.",
  contact: "Fredrik Sund",
  contactEmail: "fredrik@orbitlabs.io",
  status: "AWAITING_CUSTOMER",
  category: "BILLING",
  priority: "HIGH",
  severity: "MAJOR",
  channel: "EMAIL",
  assignee: "Daan Visser",
  received: "Apr 17, 09:15",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: null,
  unread: 0,
  anonymous: false
}, {
  id: "C-20260416-0004",
  subject: "DSR export not received after 30 days",
  customer: "Aker Solutions AS",
  contact: "Ingrid Bakke",
  contactEmail: "ingrid@aker.no",
  status: "IN_RESOLUTION",
  category: "PRIVACY",
  priority: "URGENT",
  severity: "MAJOR",
  channel: "LETTER",
  assignee: "Priya Shah",
  received: "Apr 16, 11:32",
  slaFRTBreach: true,
  slaResBreach: true,
  slaResMinutesLeft: -180,
  unread: 2,
  anonymous: false
}, {
  id: "C-20260415-0029",
  subject: "Onboarding wizard step 3 confusing",
  customer: "Tidewater Games",
  contact: "Jonas Weber",
  contactEmail: "jonas@tidewater.gg",
  status: "RESOLVED",
  category: "DELIVERY",
  priority: "LOW",
  severity: "MINOR",
  channel: "IN_APP",
  assignee: "Daan Visser",
  received: "Apr 15, 16:08",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: null,
  unread: 0,
  anonymous: false
}, {
  id: "C-20260414-0002",
  subject: "Feature X is missing from Growth plan as advertised",
  customer: "—",
  contact: "Anonymous · no email provided",
  contactEmail: null,
  status: "REJECTED",
  category: "OTHER",
  priority: "LOW",
  severity: "MINOR",
  channel: "IN_APP",
  assignee: "Daan Visser",
  received: "Apr 14, 17:55",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: null,
  unread: 0,
  anonymous: true,
  rejectionReason: "INSUFFICIENT_INFO"
}, {
  id: "C-20260402-0008",
  subject: "Cancellation flow kept me going in circles",
  customer: "Orbit Labs B.V.",
  contact: "Elin Karlsson",
  contactEmail: "elin@orbitlabs.io",
  status: "CLOSED",
  category: "DELIVERY",
  priority: "MEDIUM",
  severity: "MODERATE",
  channel: "PHONE",
  assignee: "Daan Visser",
  received: "Apr 02, 11:05",
  slaFRTBreach: false,
  slaResBreach: false,
  slaResMinutesLeft: null,
  unread: 0,
  anonymous: false,
  rootCauseCode: "journey.cancellation.friction",
  structuralIssue: true,
  csatRating: 4
}];

// ─── Shared bits ─────────────────────────────────────────────────────
function CxStatusChip({
  status
}) {
  const s = CX_STATUS[status] || {
    label: status,
    tone: "muted"
  };
  return /*#__PURE__*/React.createElement(PillLabel, {
    tone: s.tone
  }, s.label);
}
function CxPriorityPill({
  priority
}) {
  const p = CX_PRIORITIES.find(x => x.k === priority) || CX_PRIORITIES[2];
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      padding: "2px 8px 2px 6px",
      borderRadius: 999,
      background: "color-mix(in srgb, " + p.color + " 14%, transparent)",
      color: p.color,
      font: "500 11px/16px Roboto",
      letterSpacing: "0.02em"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: p.color
    }
  }), p.label);
}
function CxLifecycle({
  current,
  compact
}) {
  // Awaiting-customer is a sub-state of IN_RESOLUTION for the visual.
  const effective = current === "AWAITING_CUSTOMER" ? "IN_RESOLUTION" : current === "REJECTED" ? "CLOSED" : current;
  const currentIdx = CX_LIFECYCLE.findIndex(s => s.k === effective);
  const rejected = current === "REJECTED";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: `repeat(${CX_LIFECYCLE.length}, 1fr)`,
      gap: compact ? 4 : 8,
      marginTop: 6
    }
  }, CX_LIFECYCLE.map((s, i) => {
    const done = i < currentIdx;
    const active = i === currentIdx;
    const color = rejected ? "var(--sp-text-subtle)" : done ? "var(--sp-accent-mint)" : active ? "#1A73E8" : "var(--sp-border)";
    return /*#__PURE__*/React.createElement("div", {
      key: s.k
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        height: compact ? 3 : 5,
        borderRadius: 4,
        background: color,
        opacity: !done && !active ? 0.6 : 1
      }
    }), !compact && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: active ? "600 11px/14px Roboto" : "500 11px/14px Roboto",
        color: active ? "#1A73E8" : done ? "var(--sp-text)" : "var(--sp-text-muted)",
        marginTop: 6
      }
    }, s.label), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, s.hint)));
  }));
}
function CxSLARow({
  slaFRT,
  slaRes,
  status
}) {
  const terminal = status === "CLOSED" || status === "REJECTED" || status === "RESOLVED";
  const cell = (label, data) => {
    const ok = data?.ok;
    const color = ok === false ? "#D93025" : ok ? "var(--sp-accent-mint)" : "var(--sp-text-muted)";
    return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        textTransform: "uppercase",
        letterSpacing: "0.06em"
      }
    }, label), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "600 15px/20px Roboto",
        color,
        marginTop: 4
      }
    }, data?.hit ? `✓ Hit in ${data.hit}` : data?.due ? `Due ${data.due}` : terminal ? "—" : "Pending"));
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 20
    }
  }, cell("First response", slaFRT), cell("Resolution", slaRes));
}

// ────────────────────────────────────────────────────────────────────────
// PORTAL — my complaints list
// ────────────────────────────────────────────────────────────────────────
function PComplaints({
  state,
  onOpen,
  onNew
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 220,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 4
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  const items = state === "empty" ? [] : MY_COMPLAINTS;
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 280
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 32px/38px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.02em"
    }
  }, "Complaints"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 15px/22px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6,
      maxWidth: 620
    }
  }, "Formal complaints about your service, billing, or staff conduct. Each complaint is tracked through a regulated lifecycle and a case-handler is assigned to you personally.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: onNew,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "File a complaint")), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 28
    }
  }), items.length === 0 ? /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25C9",
    title: "No complaints on file",
    body: "When something goes wrong, a complaint creates a formal record with a guaranteed response time and a dedicated case-handler. Different from a support ticket.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: onNew
    }, "File a complaint")
  }) : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, items.map(c => {
    const p = CX_PRIORITIES.find(x => x.k === c.priority);
    const cat = CX_CATEGORIES.find(x => x.k === c.category);
    const canRate = c.status === "CLOSED" && !c.csatRating;
    return /*#__PURE__*/React.createElement(PCard, {
      key: c.id,
      pad: 0,
      style: {
        cursor: "pointer"
      },
      onClick: () => onOpen(c)
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        padding: 20
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "flex-start",
        gap: 12,
        flexWrap: "wrap"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 240
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8,
        flexWrap: "wrap",
        marginBottom: 6
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 12px/16px 'Roboto Mono',monospace",
        color: "var(--sp-text-subtle)"
      }
    }, c.id), /*#__PURE__*/React.createElement(CxStatusChip, {
      status: c.status
    }), canRate && /*#__PURE__*/React.createElement(PillLabel, {
      tone: "info"
    }, "Rate resolution \u2192")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "600 17px/24px Roboto",
        color: "var(--sp-text)"
      }
    }, c.subject), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 6,
        display: "flex",
        gap: 10,
        flexWrap: "wrap"
      }
    }, /*#__PURE__*/React.createElement("span", null, cat?.label), /*#__PURE__*/React.createElement("span", null, "\xB7"), /*#__PURE__*/React.createElement("span", null, "Filed ", c.received), c.assignee && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("span", null, "\xB7"), /*#__PURE__*/React.createElement("span", null, "Handler: ", c.assignee)))), /*#__PURE__*/React.createElement(CxPriorityPill, {
      priority: c.priority
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 16
      }
    }, /*#__PURE__*/React.createElement(CxLifecycle, {
      current: c.status,
      compact: true
    }))));
  })));
}

// ────────────────────────────────────────────────────────────────────────
// PORTAL — complaint detail (read-mostly; reply composer + reopen)
// ────────────────────────────────────────────────────────────────────────
function PComplaint({
  complaint,
  onBack
}) {
  const c = complaint;
  const cat = CX_CATEGORIES.find(x => x.k === c.category);
  const [rating, setRating] = React.useState(c.csatRating || 0);
  const [reply, setReply] = React.useState("");
  const p = CX_PRIORITIES.find(x => x.k === c.priority);
  const canReply = c.status !== "CLOSED" && c.status !== "REJECTED";
  const canRate = c.status === "CLOSED" && !c.csatRating;
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 16,
      display: "inline-block"
    }
  }, "\u2190 Back to complaints"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, c.id), /*#__PURE__*/React.createElement(CxStatusChip, {
    status: c.status
  }), /*#__PURE__*/React.createElement(CxPriorityPill, {
    priority: c.priority
  })), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, c.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement(CxLifecycle, {
    current: c.status
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 24,
      marginTop: 28,
      padding: "20px 0",
      borderTop: "1px solid var(--sp-border)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "Category"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, cat?.label)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "Case-handler"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, c.assignee || "Not yet assigned"), c.assigneeRole && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.assigneeRole)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "Filed"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, c.received)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "SLA"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, "First response: ", p?.frt, " \xB7 Resolution: ", p?.res))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement(SideLabel, null, "Your complaint"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/22px Roboto",
      color: "var(--sp-text)",
      marginTop: 8,
      whiteSpace: "pre-wrap"
    }
  }, c.description))), c.compensation && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16,
      background: "linear-gradient(135deg, rgba(0,184,148,0.06), transparent)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Resolution offered"), /*#__PURE__*/React.createElement(PillLabel, {
    tone: "mint"
  }, c.compensation.kind.replace(/_/g, " ")), c.compensation.amount && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-accent-mint)"
    }
  }, fmtMoney(c.compensation.amount))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/22px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.compensation.note)), canRate && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "How did we do?"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 14
    }
  }, "Rate the resolution from 1 (poor) to 5 (excellent). Your rating helps us improve."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, [1, 2, 3, 4, 5].map(i => /*#__PURE__*/React.createElement("span", {
    key: i,
    onClick: () => setRating(i),
    style: {
      width: 44,
      height: 44,
      borderRadius: 8,
      cursor: "pointer",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      background: rating >= i ? "#F9A825" : "var(--sp-surface-2)",
      border: "1px solid var(--sp-border)",
      color: rating >= i ? "#fff" : "var(--sp-text-muted)",
      font: "600 18px/1 Roboto"
    }
  }, "\u2605")))), c.status === "CLOSED" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 260
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Not fully resolved?"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "You can reopen this complaint within 14 days of closure. A handler will pick it up again.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Reopen complaint"))), canReply && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Add information"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "\xB7 your case-handler will see this immediately")), /*#__PURE__*/React.createElement("textarea", {
    value: reply,
    onChange: e => setReply(e.target.value),
    placeholder: "Type your message\u2026",
    style: {
      width: "100%",
      minHeight: 96,
      resize: "vertical",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: 12,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    disabled: !reply.trim()
  }, "Send"))));
}

// ────────────────────────────────────────────────────────────────────────
// COMPLAINT FORM — reused by portal (authed), anonymous page (no login),
// and admin (agent-logged intake). Renders an identical field set; the
// `mode` prop toggles the identity block.
// ────────────────────────────────────────────────────────────────────────
function ComplaintForm({
  mode,
  onSubmit,
  onCancel
}) {
  // mode: "authed" | "anon" | "agent"
  const [category, setCategory] = React.useState("BILLING");
  const [priority, setPriority] = React.useState("MEDIUM");
  const [subject, setSubject] = React.useState("");
  const [desc, setDesc] = React.useState("");
  // Anonymous identity
  const [anonName, setAnonName] = React.useState("");
  const [anonEmail, setAnonEmail] = React.useState("");
  const [anonPhone, setAnonPhone] = React.useState("");
  const [anonConsent, setAnonConsent] = React.useState(false);
  // Authed contextual links
  const [relates, setRelates] = React.useState("none"); // none / subscription / invoice

  const canSubmit = subject.trim() && desc.trim() && (mode !== "anon" || anonConsent && (anonEmail.trim() || anonPhone.trim()) && anonName.trim());
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "What is this about?"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
      gap: 8
    }
  }, CX_CATEGORIES.map(c => {
    const on = category === c.k;
    return /*#__PURE__*/React.createElement("div", {
      key: c.k,
      onClick: () => setCategory(c.k),
      style: {
        padding: "12px 14px",
        borderRadius: 8,
        cursor: "pointer",
        border: "1px solid " + (on ? "#1A73E8" : "var(--sp-border)"),
        background: on ? "rgba(26,115,232,0.06)" : "var(--sp-surface)",
        font: "500 14px/20px Roboto",
        color: on ? "#1A73E8" : "var(--sp-text)"
      }
    }, c.label);
  }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 8
    }
  }, "Summary"), /*#__PURE__*/React.createElement("input", {
    value: subject,
    onChange: e => setSubject(e.target.value),
    placeholder: "One sentence describing the issue",
    style: {
      width: "100%",
      padding: "12px 14px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 8
    }
  }, "What happened?"), /*#__PURE__*/React.createElement("textarea", {
    value: desc,
    onChange: e => setDesc(e.target.value),
    rows: 6,
    placeholder: "Tell us what went wrong and what outcome you're looking for. The more detail, the faster we can help.",
    style: {
      width: "100%",
      padding: "12px 14px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none",
      resize: "vertical"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 6
    }
  }, desc.length, " characters \xB7 max 4,000")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "How urgent is this?"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
      gap: 8
    }
  }, CX_PRIORITIES.map(p => {
    const on = priority === p.k;
    return /*#__PURE__*/React.createElement("div", {
      key: p.k,
      onClick: () => setPriority(p.k),
      style: {
        padding: 12,
        borderRadius: 8,
        cursor: "pointer",
        border: "1px solid " + (on ? p.color : "var(--sp-border)"),
        background: on ? "color-mix(in srgb, " + p.color + " 8%, transparent)" : "var(--sp-surface)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 8,
        height: 8,
        borderRadius: "50%",
        background: p.color
      }
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, p.label)), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 4
      }
    }, "First response in ", p.frt, " \xB7 resolution in ", p.res));
  })), mode !== "agent" && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 6
    }
  }, "Your case-handler may re-assess priority after review.")), mode === "anon" && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 20,
      borderRadius: 12,
      background: "var(--sp-surface-2)",
      border: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 4
    }
  }, "How should we reach you?"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 14
    }
  }, "You don't need an Incedo account to file a complaint. We just need a way to reply. Your details are used only for this case."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      marginBottom: 6
    }
  }, "Your name *"), /*#__PURE__*/React.createElement("input", {
    value: anonName,
    onChange: e => setAnonName(e.target.value),
    placeholder: "Full name",
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      marginBottom: 6
    }
  }, "Email"), /*#__PURE__*/React.createElement("input", {
    value: anonEmail,
    onChange: e => setAnonEmail(e.target.value),
    placeholder: "you@example.com",
    type: "email",
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("label", {
    style: {
      display: "block",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      marginBottom: 6
    }
  }, "Phone"), /*#__PURE__*/React.createElement("input", {
    value: anonPhone,
    onChange: e => setAnonPhone(e.target.value),
    placeholder: "+31 6 \u2026",
    type: "tel",
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Provide at least one (email or phone). If both are given, we'll default to email.")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      gap: 10,
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("input", {
    id: "consent",
    type: "checkbox",
    checked: anonConsent,
    onChange: e => setAnonConsent(e.target.checked),
    style: {
      marginTop: 3
    }
  }), /*#__PURE__*/React.createElement("label", {
    htmlFor: "consent",
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      cursor: "pointer"
    }
  }, "I consent to Incedo processing the details above to handle this complaint, in line with the ", /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      color: "#1A73E8"
    }
  }, "privacy policy"), ". I can withdraw consent by emailing privacy@incedo.nl."))), mode === "authed" && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "Does this relate to something specific?"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
      gap: 8
    }
  }, [{
    k: "none",
    label: "Nothing specific"
  }, {
    k: "subscription",
    label: "My subscription"
  }, {
    k: "invoice",
    label: "An invoice"
  }].map(o => {
    const on = relates === o.k;
    return /*#__PURE__*/React.createElement("div", {
      key: o.k,
      onClick: () => setRelates(o.k),
      style: {
        padding: 12,
        borderRadius: 8,
        cursor: "pointer",
        border: "1px solid " + (on ? "#1A73E8" : "var(--sp-border)"),
        background: on ? "rgba(26,115,232,0.06)" : "var(--sp-surface)",
        font: "500 13px/18px Roboto",
        color: on ? "#1A73E8" : "var(--sp-text)"
      }
    }, o.label);
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: 10,
      paddingTop: 10,
      borderTop: "1px solid var(--sp-border)"
    }
  }, onCancel && /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    onClick: onCancel
  }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    disabled: !canSubmit,
    onClick: () => onSubmit({
      mode,
      category,
      priority,
      subject,
      description: desc,
      identity: mode === "anon" ? {
        name: anonName,
        email: anonEmail,
        phone: anonPhone
      } : null,
      relates
    })
  }, "File complaint")));
}
function PNewComplaint({
  onCancel,
  onSubmit
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onCancel,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 16,
      display: "inline-block"
    }
  }, "\u2190 Back to complaints"), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.02em"
    }
  }, "File a complaint"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6,
      marginBottom: 24,
      maxWidth: 620
    }
  }, "Complaints are handled differently from support tickets: a dedicated case-handler is assigned, resolution times are guaranteed by SLA, and your feedback feeds back into our product decisions."), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(ComplaintForm, {
    mode: "authed",
    onSubmit: onSubmit,
    onCancel: onCancel
  })));
}

// ────────────────────────────────────────────────────────────────────────
// ADMIN — complaints queue + detail
// ────────────────────────────────────────────────────────────────────────
function ComplaintsAdmin({
  state,
  onOpen,
  onNew
}) {
  const [status, setStatus] = React.useState("open"); // open / all / closed / mine
  const [priority, setPriority] = React.useState("all");
  const [q, setQ] = React.useState("");
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 240,
    h: 40
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 6
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  const list = ADMIN_COMPLAINTS.filter(c => {
    if (status === "open" && (c.status === "CLOSED" || c.status === "REJECTED")) return false;
    if (status === "closed" && c.status !== "CLOSED" && c.status !== "REJECTED") return false;
    if (status === "mine" && c.assignee !== "Priya Shah") return false;
    if (priority !== "all" && c.priority !== priority) return false;
    if (q && !(c.subject.toLowerCase().includes(q.toLowerCase()) || c.id.toLowerCase().includes(q.toLowerCase()) || c.customer.toLowerCase().includes(q.toLowerCase()))) return false;
    return true;
  });
  const breached = ADMIN_COMPLAINTS.filter(c => c.slaResBreach).length;
  const urgent = ADMIN_COMPLAINTS.filter(c => c.priority === "URGENT" && c.status !== "CLOSED" && c.status !== "REJECTED").length;
  const unassigned = ADMIN_COMPLAINTS.filter(c => c.assignee === "—" && c.status !== "CLOSED" && c.status !== "REJECTED").length;
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Complaints"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Formal customer-dissatisfaction records with SLA tracking and root-cause analytics.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: onNew,
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Log a complaint")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
      gap: 14,
      marginTop: 24
    }
  }, [{
    label: "Open",
    value: ADMIN_COMPLAINTS.filter(c => c.status !== "CLOSED" && c.status !== "REJECTED").length,
    tone: "info"
  }, {
    label: "SLA breached",
    value: breached,
    tone: breached > 0 ? "warm" : "muted"
  }, {
    label: "Urgent",
    value: urgent,
    tone: urgent > 0 ? "warm" : "muted"
  }, {
    label: "Unassigned",
    value: unassigned,
    tone: unassigned > 0 ? "amber" : "muted"
  }].map(k => /*#__PURE__*/React.createElement(PCard, {
    key: k.label
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, k.label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: k.tone === "warm" ? "#D93025" : k.tone === "amber" ? "#E8710A" : "var(--sp-text)",
      marginTop: 6
    }
  }, k.value)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginTop: 24,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: status,
    onChange: setStatus,
    options: [{
      value: "open",
      label: "Open"
    }, {
      value: "mine",
      label: "Mine"
    }, {
      value: "closed",
      label: "Closed"
    }, {
      value: "all",
      label: "All"
    }]
  }), /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: priority,
    onChange: setPriority,
    options: [{
      value: "all",
      label: "Any"
    }, {
      value: "URGENT",
      label: "Urgent"
    }, {
      value: "HIGH",
      label: "High"
    }, {
      value: "MEDIUM",
      label: "Medium"
    }, {
      value: "LOW",
      label: "Low"
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("input", {
    value: q,
    onChange: e => setQ(e.target.value),
    placeholder: "Search ID, subject, customer\u2026",
    style: {
      width: 280,
      padding: "8px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      marginTop: 16,
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "160px 1fr 180px 120px 120px 120px 140px 40px",
      gap: 12,
      padding: "10px 16px",
      borderBottom: "1px solid var(--sp-border)",
      background: "var(--sp-surface-2)",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, /*#__PURE__*/React.createElement("span", null, "ID"), /*#__PURE__*/React.createElement("span", null, "Subject"), /*#__PURE__*/React.createElement("span", null, "Customer"), /*#__PURE__*/React.createElement("span", null, "Priority"), /*#__PURE__*/React.createElement("span", null, "Status"), /*#__PURE__*/React.createElement("span", null, "SLA"), /*#__PURE__*/React.createElement("span", null, "Assignee"), /*#__PURE__*/React.createElement("span", null)), list.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "No complaints match.") : list.map(c => {
    const slaBad = c.slaResBreach || c.slaFRTBreach;
    const slaWarn = !slaBad && c.slaResMinutesLeft !== null && c.slaResMinutesLeft < 240 && c.status !== "CLOSED" && c.status !== "REJECTED" && c.status !== "RESOLVED";
    return /*#__PURE__*/React.createElement("div", {
      key: c.id,
      onClick: () => onOpen(c),
      style: {
        display: "grid",
        gridTemplateColumns: "160px 1fr 180px 120px 120px 120px 140px 40px",
        gap: 12,
        padding: "14px 16px",
        alignItems: "center",
        borderBottom: "1px solid var(--sp-border)",
        cursor: "pointer",
        background: c.unread > 0 ? "color-mix(in srgb, #1A73E8 3%, transparent)" : "transparent"
      },
      onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
      onMouseLeave: e => e.currentTarget.style.background = c.unread > 0 ? "color-mix(in srgb, #1A73E8 3%, transparent)" : "transparent"
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 12px/16px 'Roboto Mono',monospace",
        color: "var(--sp-text-subtle)"
      }
    }, c.id.replace(/^C-/, "")), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.subject), c.unread > 0 && /*#__PURE__*/React.createElement("span", {
      style: {
        display: "inline-block",
        width: 7,
        height: 7,
        borderRadius: "50%",
        background: "#1A73E8",
        flex: "none"
      }
    }), c.anonymous && /*#__PURE__*/React.createElement(PillLabel, {
      tone: "muted"
    }, "Anonymous")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, CX_CATEGORIES.find(x => x.k === c.category)?.label, " \xB7 filed ", c.received)), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text)",
        minWidth: 0,
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, /*#__PURE__*/React.createElement("div", null, c.customer), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, c.contact)), /*#__PURE__*/React.createElement(CxPriorityPill, {
      priority: c.priority
    }), /*#__PURE__*/React.createElement(CxStatusChip, {
      status: c.status
    }), /*#__PURE__*/React.createElement("div", null, slaBad ? /*#__PURE__*/React.createElement(PillLabel, {
      tone: "warm"
    }, "Breached") : slaWarn ? /*#__PURE__*/React.createElement(PillLabel, {
      tone: "amber"
    }, Math.round(c.slaResMinutesLeft / 60), "h left") : c.status === "CLOSED" || c.status === "REJECTED" || c.status === "RESOLVED" ? /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, "\u2014") : /*#__PURE__*/React.createElement(PillLabel, {
      tone: "muted"
    }, c.slaResMinutesLeft !== null ? Math.round(c.slaResMinutesLeft / 60) + "h left" : "on track")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 13px/18px Roboto",
        color: c.assignee === "—" ? "#E8710A" : "var(--sp-text-muted)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.assignee), /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-subtle)",
        textAlign: "right"
      }
    }, "\u203A"));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", list.length, " of ", ADMIN_COMPLAINTS.length, " complaints."));
}
function ComplaintDetailAdmin({
  complaint,
  onBack,
  onOpenCustomer
}) {
  const c = complaint;
  const cat = CX_CATEGORIES.find(x => x.k === c.category);
  const p = CX_PRIORITIES.find(x => x.k === c.priority);
  const [note, setNote] = React.useState("");
  const [rootCause, setRootCause] = React.useState(c.rootCauseCode || "");
  const [showResolve, setShowResolve] = React.useState(false);
  const events = [{
    t: "Received",
    time: c.received,
    who: c.anonymous ? c.contact : c.contact,
    body: c.subject + " · via " + (CX_CHANNELS.find(x => x.k === c.channel)?.label || c.channel),
    channel: c.channel
  }, c.status !== "RECEIVED" && {
    t: "Registered",
    time: "+2m",
    who: c.assignee !== "—" ? c.assignee : "System",
    body: `Categorised as ${cat?.label}. CDP suppression active.`
  }, (c.status === "UNDER_REVIEW" || c.status === "IN_RESOLUTION" || c.status === "AWAITING_CUSTOMER" || c.status === "RESOLVED" || c.status === "CLOSED") && {
    t: "Assessed",
    time: "+18m",
    who: c.assignee,
    body: `Priority ${p?.label}. First response due in ${p?.frt}, resolution in ${p?.res}.`
  }, (c.status === "IN_RESOLUTION" || c.status === "AWAITING_CUSTOMER" || c.status === "RESOLVED" || c.status === "CLOSED") && {
    t: "Reply sent",
    time: "+1h 30m",
    who: c.assignee,
    body: "Acknowledgement sent to customer via email.",
    channel: "EMAIL"
  }, c.status === "AWAITING_CUSTOMER" && {
    t: "Awaiting customer",
    time: "2d ago",
    who: c.assignee,
    body: "Requested bank statement copy. Resolution clock paused."
  }, (c.status === "RESOLVED" || c.status === "CLOSED") && {
    t: "Resolved",
    time: c.status === "CLOSED" ? "Apr 04, 16:20" : "Yesterday",
    who: c.assignee,
    body: "Refund processed. Customer notified."
  }, c.status === "CLOSED" && {
    t: "Closed",
    time: "Apr 05, 09:12",
    who: c.assignee,
    body: `Root cause: ${c.rootCauseCode || "(not yet tagged)"}${c.structuralIssue ? " · structural issue" : ""}.`
  }, c.status === "REJECTED" && {
    t: "Rejected",
    time: "—",
    who: c.assignee,
    body: `Reason: ${c.rejectionReason || "—"}`
  }].filter(Boolean);
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back to complaints"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 320px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, c.id), /*#__PURE__*/React.createElement(CxStatusChip, {
    status: c.status
  }), /*#__PURE__*/React.createElement(CxPriorityPill, {
    priority: c.priority
  }), c.anonymous && /*#__PURE__*/React.createElement(PillLabel, {
    tone: "muted"
  }, "Anonymous intake"), c.slaResBreach && /*#__PURE__*/React.createElement(PillLabel, {
    tone: "warm"
  }, "SLA breached")), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 22px/28px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, c.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 8
    }
  }, cat?.label, " \xB7 filed ", c.received, " \xB7 via ", CX_CHANNELS.find(x => x.k === c.channel)?.label), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 22
    }
  }, /*#__PURE__*/React.createElement(CxLifecycle, {
    current: c.status
  }))), /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 16
    }
  }, "Case timeline"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 18
    }
  }, events.map((ev, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 120,
      flex: "none",
      font: "500 12px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("div", null, ev.time), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 2
    }
  }, ev.who)), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 2,
      background: "var(--sp-border)",
      flex: "none",
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      left: -5,
      top: 4,
      width: 12,
      height: 12,
      borderRadius: "50%",
      background: "var(--sp-surface)",
      border: "2px solid #1A73E8"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      paddingBottom: 2
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, ev.t), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, ev.body))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      paddingTop: 16,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "Add internal note or reply"), /*#__PURE__*/React.createElement("textarea", {
    value: note,
    onChange: e => setNote(e.target.value),
    rows: 4,
    placeholder: "Customer-facing reply or internal note\u2026",
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none",
      resize: "vertical"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: 8,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Internal note"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    disabled: !note.trim()
  }, "Reply to customer")))), (c.status === "IN_RESOLUTION" || c.status === "AWAITING_CUSTOMER" || c.status === "UNDER_REVIEW") && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start",
      gap: 12,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Propose resolution"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Summary + optional compensation. Customer is notified on submit.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => setShowResolve(s => !s)
  }, showResolve ? "Hide" : "Draft resolution")), showResolve && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16,
      display: "flex",
      flexDirection: "column",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("textarea", {
    rows: 3,
    placeholder: "What did we do to resolve this?",
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none",
      resize: "vertical"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 160px",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("select", {
    style: {
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("option", null, "NONE \u2014 no compensation"), /*#__PURE__*/React.createElement("option", null, "APOLOGY \u2014 written apology"), /*#__PURE__*/React.createElement("option", null, "GOODWILL_CREDIT \u2014 account credit"), /*#__PURE__*/React.createElement("option", null, "REFUND \u2014 money back"), /*#__PURE__*/React.createElement("option", null, "REPLACEMENT \u2014 replace service/product"), /*#__PURE__*/React.createElement("option", null, "SERVICE_UPGRADE \u2014 temporary upgrade")), /*#__PURE__*/React.createElement("input", {
    placeholder: "Amount (EUR)",
    type: "number",
    style: {
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm"
  }, "Propose & notify")))), c.status === "RESOLVED" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "Close & tag root cause"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 14
    }
  }, "Closing requires a root-cause code so analytics can roll up patterns."), /*#__PURE__*/React.createElement("select", {
    value: rootCause,
    onChange: e => setRootCause(e.target.value),
    style: {
      width: "100%",
      padding: "10px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("option", {
    value: ""
  }, "Select a root cause\u2026"), CX_ROOT_CAUSES.map(r => /*#__PURE__*/React.createElement("option", {
    key: r.k,
    value: r.k
  }, r.label))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("input", {
    id: "struct",
    type: "checkbox"
  }), /*#__PURE__*/React.createElement("label", {
    htmlFor: "struct",
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "This is a structural issue (not a one-off)")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "flex-end",
      gap: 8,
      marginTop: 14
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    disabled: !rootCause
  }, "Close complaint")))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "SLA"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(CxSLARow, {
    slaFRT: null,
    slaRes: {
      ok: !c.slaResBreach,
      due: c.slaResMinutesLeft !== null ? c.slaResMinutesLeft > 0 ? "in " + Math.round(c.slaResMinutesLeft / 60) + "h" : Math.abs(Math.round(c.slaResMinutesLeft / 60)) + "h ago" : null
    },
    status: c.status
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Customer"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, c.customer !== "—" ? c.customer : "—"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, c.contact), c.contactEmail && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "#1A73E8",
      marginTop: 2
    }
  }, c.contactEmail), c.customer !== "—" && onOpenCustomer && /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      marginTop: 10,
      padding: 0
    },
    onClick: () => onOpenCustomer(c.customer)
  }, "Open Customer 360 \u2192"), c.anonymous && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      padding: "8px 10px",
      borderRadius: 6,
      background: "var(--sp-surface-2)",
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Anonymous intake. Identity is self-reported and not linked to an Incedo account.")), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Assignee"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: c.assignee === "—" ? "#E8710A" : "var(--sp-text)",
      marginTop: 6
    }
  }, c.assignee === "—" ? "Not yet assigned" : c.assignee), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      marginTop: 8,
      padding: 0
    }
  }, "Reassign")), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Suppression"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, c.status === "CLOSED" || c.status === "REJECTED" ? "Lifted" : "Marketing-only"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, c.status === "CLOSED" || c.status === "REJECTED" ? "Outbound campaigns resumed." : "No marketing outbound until closed. Transactional mail still flows.")), !c.anonymous && /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Related"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6,
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      font: "500 13px/18px Roboto",
      color: "#1A73E8",
      textDecoration: "none"
    }
  }, "Subscription SUB-00421"), /*#__PURE__*/React.createElement("a", {
    href: "#",
    style: {
      font: "500 13px/18px Roboto",
      color: "#1A73E8",
      textDecoration: "none"
    }
  }, "Invoice INV-20251002"))))));
}
function NewComplaintAdmin({
  onCancel,
  onSubmit
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onCancel,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back to complaints"), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Log a complaint"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 20,
      maxWidth: 640
    }
  }, "Use this to log a complaint received by phone, letter, or in-person. For customer-submitted complaints (portal / email / chat), the record is created automatically by the intake adapter."), /*#__PURE__*/React.createElement(PCard, {
    style: {
      maxWidth: 820
    }
  }, /*#__PURE__*/React.createElement(ComplaintForm, {
    mode: "agent",
    onSubmit: onSubmit,
    onCancel: onCancel
  })));
}
Object.assign(window, {
  // Taxonomies
  CX_CATEGORIES,
  CX_PRIORITIES,
  CX_STATUS,
  CX_LIFECYCLE,
  CX_CHANNELS,
  CX_ROOT_CAUSES,
  // Data
  MY_COMPLAINTS,
  ADMIN_COMPLAINTS,
  // Shared UI
  CxStatusChip,
  CxPriorityPill,
  CxLifecycle,
  CxSLARow,
  // Portal
  PComplaints,
  PComplaint,
  PNewComplaint,
  // Admin
  ComplaintsAdmin,
  ComplaintDetailAdmin,
  NewComplaintAdmin,
  // Shared form
  ComplaintForm
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/complaints.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/contacts_activities.jsx
try { (() => {
// ─────────────────────────────────────────────────────────────────────────
// Contacts + Activities
//
// Contacts: list of people with their company, role, email, phone, owner.
// Contacts whose company is in CUSTOMER_PROFILES or SUBS get the "customer"
// flag — clicking them opens Customer 360 (scoped to the highlighted contact).
// Other contacts open a lightweight Contact detail screen.
//
// Activities: a log of calls/emails/meetings/tasks across all customers,
// with filters by kind, owner, and status.
// ─────────────────────────────────────────────────────────────────────────

// Build a set of customer companies by merging CUSTOMER_PROFILES + SUBS.
function _customerCompanies() {
  const set = new Set();
  try {
    (SUBS || []).forEach(s => set.add(s.customer));
  } catch {}
  try {
    Object.keys(CUSTOMER_PROFILES || {}).forEach(k => set.add(k));
  } catch {}
  return set;
}

// ─── Fixture ────────────────────────────────────────────────────────────
const CONTACTS = [
// Orbit Labs — linked (rich profile)
{
  id: "ct-001",
  name: "Elin Karlsson",
  company: "Orbit Labs B.V.",
  role: "COO",
  email: "elin@orbitlabs.io",
  phone: "+31 6 2211 4455",
  owner: "Bram de Vries",
  tags: ["decision-maker", "eu-north"],
  last: "Today",
  status: "active"
}, {
  id: "ct-002",
  name: "Fredrik Sund",
  company: "Orbit Labs B.V.",
  role: "Finance lead",
  email: "fredrik@orbitlabs.io",
  phone: "+31 6 4811 0912",
  owner: "Bram de Vries",
  tags: ["billing"],
  last: "Apr 18",
  status: "active"
}, {
  id: "ct-003",
  name: "Matilda Järvinen",
  company: "Orbit Labs B.V.",
  role: "Admin",
  email: "matilda@orbitlabs.io",
  phone: null,
  owner: "Bram de Vries",
  tags: [],
  last: "Mar 02",
  status: "active"
},
// Northwind — linked
{
  id: "ct-010",
  name: "Heinrich Vogel",
  company: "Northwind GmbH",
  role: "CFO",
  email: "h.vogel@northwind.de",
  phone: "+49 89 4412 7781",
  owner: "Anna Krause",
  tags: ["at-risk", "dach"],
  last: "Yesterday",
  status: "active"
}, {
  id: "ct-011",
  name: "Lena Krause",
  company: "Northwind GmbH",
  role: "Procurement",
  email: "l.krause@northwind.de",
  phone: null,
  owner: "Anna Krause",
  tags: [],
  last: "Mar 14",
  status: "active"
},
// Other subs customers — linked but no rich profile (Customer 360 falls back)
{
  id: "ct-020",
  name: "Ruben Jansen",
  company: "Acme Holdings",
  role: "VP Engineering",
  email: "ruben@acme.com",
  phone: "+31 20 551 2244",
  owner: "Anna Krause",
  tags: ["enterprise", "champion"],
  last: "2d ago",
  status: "active"
}, {
  id: "ct-021",
  name: "Saskia Vermeer",
  company: "Acme Holdings",
  role: "CFO",
  email: "s.vermeer@acme.com",
  phone: "+31 20 551 2245",
  owner: "Anna Krause",
  tags: ["finance"],
  last: "Oct 10",
  status: "active"
}, {
  id: "ct-022",
  name: "Victor Huang",
  company: "Peregrine AI",
  role: "CTO",
  email: "victor@peregrine.ai",
  phone: "+1 415 772 0014",
  owner: "Chiara Romano",
  tags: ["trial", "early-adopter"],
  last: "Oct 14",
  status: "active"
}, {
  id: "ct-023",
  name: "Dieter Polder",
  company: "Polder & Co",
  role: "Owner",
  email: "dieter@polder.co",
  phone: "+31 70 221 5512",
  owner: "Bram de Vries",
  tags: ["smb"],
  last: "Oct 08",
  status: "active"
}, {
  id: "ct-024",
  name: "Femke de Wit",
  company: "Hanzeborg NV",
  role: "Head of Ops",
  email: "femke@hanzeborg.nl",
  phone: "+31 50 117 9981",
  owner: "Bram de Vries",
  tags: ["expansion"],
  last: "Oct 02",
  status: "active"
}, {
  id: "ct-025",
  name: "Rafael Duarte",
  company: "Kairos Mobility",
  role: "Procurement",
  email: "rafael@kairos.pt",
  phone: "+351 21 221 0012",
  owner: "Chiara Romano",
  tags: [],
  last: "Sep 28",
  status: "active"
}, {
  id: "ct-026",
  name: "Julia Marchetti",
  company: "Lumen Studios",
  role: "Creative Director",
  email: "julia@lumenstudios.it",
  phone: "+39 02 5511 4499",
  owner: "Chiara Romano",
  tags: ["design"],
  last: "Sep 21",
  status: "active"
}, {
  id: "ct-027",
  name: "Ola Lindqvist",
  company: "Norrsken AB",
  role: "COO",
  email: "ola@norrsken.se",
  phone: "+46 8 4411 0022",
  owner: "Anna Krause",
  tags: ["eu-north"],
  last: "Sep 14",
  status: "active"
}, {
  id: "ct-028",
  name: "Amira Haddad",
  company: "Meridian Fintech",
  role: "VP Finance",
  email: "a.haddad@meridian.fi",
  phone: "+358 9 4141 0022",
  owner: "Priya Shah",
  tags: ["finance", "renewal"],
  last: "Yesterday",
  status: "active"
},
// Non-customer prospects (open a Contact detail, not Customer 360)
{
  id: "ct-100",
  name: "Marlon Sørensen",
  company: "Arcwell Robotics",
  role: "Head of Product",
  email: "marlon@arcwell.io",
  phone: "+45 70 221 3344",
  owner: "Bram de Vries",
  tags: ["prospect", "meeting-booked"],
  last: "Today",
  status: "prospect"
}, {
  id: "ct-101",
  name: "Irene Costa",
  company: "Beacon Logistics",
  role: "Ops manager",
  email: "irene@beaconlog.es",
  phone: "+34 93 331 7788",
  owner: "Chiara Romano",
  tags: ["prospect"],
  last: "Apr 16",
  status: "prospect"
}, {
  id: "ct-102",
  name: "Thomas Verwey",
  company: "Vanguard Pensions",
  role: "Director IT",
  email: "t.verwey@vanguardpens.nl",
  phone: "+31 40 118 0099",
  owner: "Anna Krause",
  tags: ["prospect", "enterprise"],
  last: "Apr 12",
  status: "prospect"
}, {
  id: "ct-103",
  name: "Greta Ostermann",
  company: "Bauer & Söhne",
  role: "Procurement",
  email: "greta@bauer-soehne.de",
  phone: null,
  owner: "Anna Krause",
  tags: ["lead"],
  last: "Mar 28",
  status: "prospect"
}, {
  id: "ct-104",
  name: "Nikita Volkov",
  company: "—",
  role: "Freelance developer",
  email: "nikita.v@mail.com",
  phone: null,
  owner: "Chiara Romano",
  tags: ["individual"],
  last: "Mar 22",
  status: "prospect"
}, {
  id: "ct-105",
  name: "Sophie Laurent",
  company: "Pomme d'Or",
  role: "Co-founder",
  email: "sophie@pommedor.fr",
  phone: "+33 1 5522 7711",
  owner: "Bram de Vries",
  tags: ["prospect"],
  last: "Mar 18",
  status: "prospect"
}];

// Activities — cross-company
const ACTIVITIES = [{
  id: "act-501",
  kind: "call",
  title: "Renewal call",
  company: "Meridian Fintech",
  contact: "Amira Haddad",
  owner: "Priya Shah",
  due: "Today 14:00",
  status: "due",
  note: "Discuss Q4 renewal + expansion seats"
}, {
  id: "act-502",
  kind: "task",
  title: "Draft refund memo",
  company: "Orbit Labs B.V.",
  contact: "Elin Karlsson",
  owner: "Priya Shah",
  due: "Today 17:00",
  status: "due",
  note: "For complaint C-20260418-0017"
}, {
  id: "act-503",
  kind: "email",
  title: "Follow up on renewal packet",
  company: "Meridian Fintech",
  contact: "Amira Haddad",
  owner: "Priya Shah",
  due: "Yesterday",
  status: "overdue",
  note: "No response after 2 attempts"
}, {
  id: "act-504",
  kind: "meeting",
  title: "Discovery — security review",
  company: "Vanguard Pensions",
  contact: "Thomas Verwey",
  owner: "Anna Krause",
  due: "Oct 22 10:30",
  status: "scheduled",
  note: "Bring SOC2 packet"
}, {
  id: "act-505",
  kind: "call",
  title: "Intro call",
  company: "Arcwell Robotics",
  contact: "Marlon Sørensen",
  owner: "Bram de Vries",
  due: "Oct 22 15:00",
  status: "scheduled",
  note: "Outbound — warm referral from Elin"
}, {
  id: "act-506",
  kind: "task",
  title: "Send SSO configuration guide",
  company: "Hanzeborg NV",
  contact: "Femke de Wit",
  owner: "Bram de Vries",
  due: "Oct 20",
  status: "due",
  note: "Requested on the onboarding call"
}, {
  id: "act-507",
  kind: "email",
  title: "Payment-failed follow-up",
  company: "Northwind GmbH",
  contact: "Heinrich Vogel",
  owner: "Anna Krause",
  due: "Oct 20",
  status: "due",
  note: "Third attempt · escalate tomorrow"
}, {
  id: "act-508",
  kind: "meeting",
  title: "QBR — Acme Holdings",
  company: "Acme Holdings",
  contact: "Ruben Jansen",
  owner: "Anna Krause",
  due: "Oct 24 09:00",
  status: "scheduled",
  note: "QBR deck in Drive"
}, {
  id: "act-509",
  kind: "note",
  title: "Logged: customer asked about EU datacenter",
  company: "Peregrine AI",
  contact: "Victor Huang",
  owner: "Chiara Romano",
  due: "Oct 19",
  status: "done",
  note: "Victor asked during the trial kickoff"
}, {
  id: "act-510",
  kind: "call",
  title: "Check-in call",
  company: "Lumen Studios",
  contact: "Julia Marchetti",
  owner: "Chiara Romano",
  due: "Oct 18",
  status: "done",
  note: "CSAT 9/10 — happy customer"
}, {
  id: "act-511",
  kind: "email",
  title: "Renewal quote sent",
  company: "Hanzeborg NV",
  contact: "Femke de Wit",
  owner: "Bram de Vries",
  due: "Oct 17",
  status: "done",
  note: "Growth plan, 28 seats, 12-month commit"
}, {
  id: "act-512",
  kind: "task",
  title: "Update MSA with procurement",
  company: "Bauer & Söhne",
  contact: "Greta Ostermann",
  owner: "Anna Krause",
  due: "Oct 16",
  status: "done",
  note: "Signed + filed"
}, {
  id: "act-513",
  kind: "meeting",
  title: "Kickoff",
  company: "Beacon Logistics",
  contact: "Irene Costa",
  owner: "Chiara Romano",
  due: "Oct 15",
  status: "done",
  note: "Good rapport, technical fit confirmed"
}, {
  id: "act-514",
  kind: "call",
  title: "Escalation — payment ops",
  company: "Northwind GmbH",
  contact: "Heinrich Vogel",
  owner: "Anna Krause",
  due: "Oct 14",
  status: "done",
  note: "Heinrich committed to wire transfer"
}];

// ─── Shared pieces ──────────────────────────────────────────────────────
const ACTIVITY_KIND = {
  call: {
    glyph: "☏",
    label: "Call",
    color: "#1A73E8"
  },
  email: {
    glyph: "✉",
    label: "Email",
    color: "#673AB7"
  },
  meeting: {
    glyph: "◪",
    label: "Meeting",
    color: "#0F9D58"
  },
  task: {
    glyph: "✓",
    label: "Task",
    color: "#E8710A"
  },
  note: {
    glyph: "◌",
    label: "Note",
    color: "#5F6368"
  }
};
const ACTIVITY_STATUS = {
  done: {
    label: "Done",
    tone: "mint"
  },
  due: {
    label: "Due",
    tone: "amber"
  },
  overdue: {
    label: "Overdue",
    tone: "warm"
  },
  scheduled: {
    label: "Scheduled",
    tone: "info"
  }
};
function ContactAvatar({
  name,
  size = 36,
  tone
}) {
  const initials = (name || "?").split(/\s+/).slice(0, 2).map(s => s[0] || "").join("").toUpperCase() || "?";
  // Deterministic color from name hash
  const palette = ["#1A73E8", "#0F9D58", "#E8710A", "#673AB7", "#D93025", "#8E24AA", "#00838F", "#3949AB"];
  let hash = 0;
  for (let i = 0; i < (name || "").length; i++) hash = hash * 31 + name.charCodeAt(i) >>> 0;
  const color = tone || palette[hash % palette.length];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: size,
      height: size,
      borderRadius: "50%",
      background: "color-mix(in srgb, " + color + " 16%, transparent)",
      color,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      font: "500 " + Math.round(size * 0.38) + "px/1 Roboto",
      letterSpacing: "-0.02em",
      flex: "none"
    }
  }, initials);
}

// ────────────────────────────────────────────────────────────────────────
// CONTACTS LIST
// ────────────────────────────────────────────────────────────────────────
function ContactsList({
  onOpenContact,
  onOpenCustomer
}) {
  const customers = _customerCompanies();
  const [q, setQ] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("all");
  const [sort, setSort] = React.useState("last");
  const filtered = CONTACTS.filter(c => {
    if (statusFilter === "customers" && !customers.has(c.company)) return false;
    if (statusFilter === "prospects" && customers.has(c.company)) return false;
    if (!q) return true;
    const s = q.toLowerCase();
    return c.name.toLowerCase().includes(s) || c.company.toLowerCase().includes(s) || c.email.toLowerCase().includes(s);
  }).sort((a, b) => sort === "name" ? a.name.localeCompare(b.name) : sort === "company" ? a.company.localeCompare(b.company) : 0);
  const counts = {
    all: CONTACTS.length,
    customers: CONTACTS.filter(c => customers.has(c.company)).length,
    prospects: CONTACTS.filter(c => !customers.has(c.company)).length
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Contacts"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, CONTACTS.length, " people across ", new Set(CONTACTS.map(c => c.company)).size, " companies.")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2193"
    })
  }, "Import"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2191"
    })
  }, "Export"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "New contact"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      alignItems: "center",
      marginTop: 24,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: statusFilter,
    onChange: setStatusFilter,
    options: [{
      value: "all",
      label: `All · ${counts.all}`
    }, {
      value: "customers",
      label: `Customers · ${counts.customers}`
    }, {
      value: "prospects",
      label: `Prospects · ${counts.prospects}`
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 260
    }
  }, /*#__PURE__*/React.createElement(PInput, {
    placeholder: "Search by name, company, or email...",
    value: q,
    onChange: setQ,
    leading: "\u2315"
  })), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Sort:"), /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: sort,
    onChange: setSort,
    options: [{
      value: "last",
      label: "Recent"
    }, {
      value: "name",
      label: "Name"
    }, {
      value: "company",
      label: "Company"
    }]
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      marginTop: 16,
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "minmax(240px, 2fr) minmax(200px, 1.4fr) minmax(180px, 1.5fr) 120px 120px 100px 28px",
      gap: 12,
      padding: "10px 20px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      background: "var(--sp-surface-2)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "Name"), /*#__PURE__*/React.createElement("span", null, "Company"), /*#__PURE__*/React.createElement("span", null, "Email"), /*#__PURE__*/React.createElement("span", null, "Owner"), /*#__PURE__*/React.createElement("span", null, "Last activity"), /*#__PURE__*/React.createElement("span", null, "Type"), /*#__PURE__*/React.createElement("span", null)), filtered.map(c => {
    const isCustomer = customers.has(c.company);
    return /*#__PURE__*/React.createElement("div", {
      key: c.id,
      onClick: () => {
        if (isCustomer && onOpenCustomer) onOpenCustomer(c.company, c);else if (onOpenContact) onOpenContact(c);
      },
      style: {
        display: "grid",
        gridTemplateColumns: "minmax(240px, 2fr) minmax(200px, 1.4fr) minmax(180px, 1.5fr) 120px 120px 100px 28px",
        gap: 12,
        alignItems: "center",
        padding: "14px 20px",
        borderBottom: "1px solid var(--sp-border)",
        cursor: "pointer",
        transition: "background 120ms"
      },
      onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
      onMouseLeave: e => e.currentTarget.style.background = "transparent"
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 12,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement(ContactAvatar, {
      name: c.name,
      size: 36
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.name), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.role))), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 6,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.company), isCustomer && /*#__PURE__*/React.createElement("span", {
      title: "Customer",
      style: {
        width: 6,
        height: 6,
        borderRadius: "50%",
        background: "var(--sp-accent-mint)",
        flex: "none"
      }
    })), c.tags.length > 0 && /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 4,
        marginTop: 3,
        flexWrap: "wrap"
      }
    }, c.tags.slice(0, 2).map(t => /*#__PURE__*/React.createElement("span", {
      key: t,
      style: {
        font: "500 10px/14px Roboto",
        color: "var(--sp-text-muted)",
        padding: "1px 6px",
        borderRadius: 3,
        background: "var(--sp-surface-2)"
      }
    }, t)))), /*#__PURE__*/React.createElement("a", {
      href: "mailto:" + c.email,
      onClick: e => e.stopPropagation(),
      style: {
        font: "400 13px/18px Roboto",
        color: "#1A73E8",
        textDecoration: "none",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, c.email), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, c.owner), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, c.last), /*#__PURE__*/React.createElement(PillLabel, {
      tone: isCustomer ? "mint" : "info"
    }, isCustomer ? "Customer" : "Prospect"), /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-subtle)",
        textAlign: "right"
      }
    }, "\u203A"));
  }), filtered.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "No contacts match your filters.")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", filtered.length, " of ", CONTACTS.length, " contacts"));
}

// ────────────────────────────────────────────────────────────────────────
// CONTACT DETAIL (for non-customer prospects only; customer contacts open
// Customer 360 instead).
// ────────────────────────────────────────────────────────────────────────
function ContactDetail({
  contact,
  onBack
}) {
  if (!contact) {
    return /*#__PURE__*/React.createElement("div", {
      className: "sp-page"
    }, /*#__PURE__*/React.createElement("span", {
      onClick: onBack,
      style: {
        cursor: "pointer",
        font: "500 13px/18px Roboto",
        color: "var(--sp-text-muted)",
        marginBottom: 12,
        display: "inline-block"
      }
    }, "\u2190 Back"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 14px/20px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "No contact selected.")));
  }
  const related = ACTIVITIES.filter(a => a.contact === contact.name);
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back to contacts"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(ContactAvatar, {
    name: contact.name,
    size: 64
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 280
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, contact.name), /*#__PURE__*/React.createElement(PillLabel, {
    tone: contact.status === "active" ? "info" : "muted"
  }, "Prospect")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, contact.role, " \xB7 ", contact.company), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 24,
      marginTop: 14,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("a", {
    href: "mailto:" + contact.email,
    style: {
      font: "400 13px/20px Roboto",
      color: "#1A73E8",
      textDecoration: "none"
    }
  }, contact.email), contact.phone && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, contact.phone)), contact.tags.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      flexWrap: "wrap",
      marginTop: 12
    }
  }, contact.tags.map(t => /*#__PURE__*/React.createElement("span", {
    key: t,
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      padding: "3px 8px",
      borderRadius: 4,
      background: "var(--sp-surface-2)",
      border: "1px solid var(--sp-border)"
    }
  }, t)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u260F"
    })
  }, "Call"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    })
  }, "Email"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Log activity")))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 320px",
      gap: 20,
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Activity (", related.length, ")"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Log")), related.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      padding: "24px 0",
      textAlign: "center"
    }
  }, "No activities logged yet.") : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, related.map(a => /*#__PURE__*/React.createElement(ActivityTimelineRow, {
    key: a.id,
    a: a
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Owner"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, contact.owner)), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Contact details"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(KV, {
    k: "Email",
    v: contact.email
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Phone",
    v: contact.phone || "—"
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Company",
    v: contact.company
  }), /*#__PURE__*/React.createElement(KV, {
    k: "Role",
    v: contact.role
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Notes"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 8
    }
  }, "No notes yet. Add one to share context with the team.")))));
}

// ────────────────────────────────────────────────────────────────────────
// ACTIVITIES
// ────────────────────────────────────────────────────────────────────────
function ActivityTimelineRow({
  a,
  onOpen
}) {
  const k = ACTIVITY_KIND[a.kind] || ACTIVITY_KIND.note;
  const s = ACTIVITY_STATUS[a.status] || ACTIVITY_STATUS.done;
  return /*#__PURE__*/React.createElement("div", {
    onClick: onOpen,
    style: {
      display: "grid",
      gridTemplateColumns: "36px 1fr 120px 100px",
      gap: 12,
      alignItems: "center",
      padding: "12px 0",
      borderBottom: "1px solid var(--sp-border)",
      cursor: onOpen ? "pointer" : "default"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 32,
      height: 32,
      borderRadius: 8,
      background: "color-mix(in srgb, " + k.color + " 12%, transparent)",
      color: k.color,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      font: "500 14px/1 Roboto"
    }
  }, k.glyph), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, a.title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, a.contact ? a.contact + " · " : "", a.company, a.note ? " · " + a.note : "")), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      textAlign: "right"
    }
  }, a.due), /*#__PURE__*/React.createElement(PillLabel, {
    tone: s.tone
  }, s.label));
}
function ActivitiesList({
  onOpenCustomer,
  onOpenContact
}) {
  const customers = _customerCompanies();
  const [kindFilter, setKindFilter] = React.useState("all");
  const [statusFilter, setStatusFilter] = React.useState("open");
  const [ownerFilter, setOwnerFilter] = React.useState("all");
  const owners = ["all", ...Array.from(new Set(ACTIVITIES.map(a => a.owner)))];
  const filtered = ACTIVITIES.filter(a => {
    if (kindFilter !== "all" && a.kind !== kindFilter) return false;
    if (statusFilter === "open" && a.status === "done") return false;
    if (statusFilter === "done" && a.status !== "done") return false;
    if (statusFilter === "overdue" && a.status !== "overdue") return false;
    if (ownerFilter !== "all" && a.owner !== ownerFilter) return false;
    return true;
  });
  const counts = {
    all: ACTIVITIES.length,
    open: ACTIVITIES.filter(a => a.status !== "done").length,
    overdue: ACTIVITIES.filter(a => a.status === "overdue").length,
    done: ACTIVITIES.filter(a => a.status === "done").length
  };
  const byKind = Object.keys(ACTIVITY_KIND).reduce((acc, k) => {
    acc[k] = ACTIVITIES.filter(a => a.kind === k).length;
    return acc;
  }, {});
  const handleOpen = a => {
    if (customers.has(a.company) && onOpenCustomer) onOpenCustomer(a.company);else if (onOpenContact) {
      const ct = CONTACTS.find(c => c.name === a.contact);
      if (ct) onOpenContact(ct);
    }
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Activities"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Calls, emails, meetings, and tasks across all accounts.")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u29C9"
    })
  }, "Calendar view"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\uFF0B"
    })
  }, "Log activity"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(140px, 1fr))",
      gap: 14,
      marginTop: 24
    }
  }, [{
    k: "Open",
    v: counts.open,
    color: "var(--sp-text)"
  }, {
    k: "Overdue",
    v: counts.overdue,
    color: counts.overdue > 0 ? "#D93025" : "var(--sp-text)"
  }, {
    k: "Done",
    v: counts.done,
    color: "var(--sp-accent-mint)"
  }, {
    k: "Total",
    v: counts.all,
    color: "var(--sp-text)"
  }].map(x => /*#__PURE__*/React.createElement(PCard, {
    key: x.k
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, x.k), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/34px Roboto",
      color: x.color,
      marginTop: 6
    }
  }, x.v)))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      alignItems: "center",
      marginTop: 24,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: statusFilter,
    onChange: setStatusFilter,
    options: [{
      value: "open",
      label: `Open · ${counts.open}`
    }, {
      value: "overdue",
      label: `Overdue · ${counts.overdue}`
    }, {
      value: "done",
      label: `Done · ${counts.done}`
    }, {
      value: "all",
      label: `All · ${counts.all}`
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 1,
      height: 20,
      background: "var(--sp-border)"
    }
  }), /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    value: kindFilter,
    onChange: setKindFilter,
    options: [{
      value: "all",
      label: "All"
    }, {
      value: "call",
      label: `Calls · ${byKind.call || 0}`
    }, {
      value: "email",
      label: `Emails · ${byKind.email || 0}`
    }, {
      value: "meeting",
      label: `Meetings · ${byKind.meeting || 0}`
    }, {
      value: "task",
      label: `Tasks · ${byKind.task || 0}`
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Owner:"), /*#__PURE__*/React.createElement("select", {
    value: ownerFilter,
    onChange: e => setOwnerFilter(e.target.value),
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      padding: "5px 10px",
      cursor: "pointer"
    }
  }, owners.map(o => /*#__PURE__*/React.createElement("option", {
    key: o,
    value: o
  }, o === "all" ? "All owners" : o)))), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      marginTop: 16,
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "36px minmax(280px, 2fr) minmax(180px, 1fr) 140px 120px 100px",
      gap: 12,
      padding: "10px 20px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      background: "var(--sp-surface-2)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("span", null), /*#__PURE__*/React.createElement("span", null, "Activity"), /*#__PURE__*/React.createElement("span", null, "Company / contact"), /*#__PURE__*/React.createElement("span", null, "Owner"), /*#__PURE__*/React.createElement("span", null, "Due"), /*#__PURE__*/React.createElement("span", null, "Status")), filtered.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "No activities match your filters.") : filtered.map(a => {
    const k = ACTIVITY_KIND[a.kind];
    const s = ACTIVITY_STATUS[a.status];
    const isCustomer = customers.has(a.company);
    return /*#__PURE__*/React.createElement("div", {
      key: a.id,
      onClick: () => handleOpen(a),
      style: {
        display: "grid",
        gridTemplateColumns: "36px minmax(280px, 2fr) minmax(180px, 1fr) 140px 120px 100px",
        gap: 12,
        alignItems: "center",
        padding: "14px 20px",
        borderBottom: "1px solid var(--sp-border)",
        cursor: "pointer",
        transition: "background 120ms"
      },
      onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
      onMouseLeave: e => e.currentTarget.style.background = "transparent"
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 32,
        height: 32,
        borderRadius: 8,
        background: "color-mix(in srgb, " + k.color + " 12%, transparent)",
        color: k.color,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        font: "500 14px/1 Roboto"
      }
    }, k.glyph), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, a.title), a.note && /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2,
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, a.note)), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 6,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)",
        overflow: "hidden",
        textOverflow: "ellipsis",
        whiteSpace: "nowrap"
      }
    }, a.company), isCustomer && /*#__PURE__*/React.createElement("span", {
      title: "Customer",
      style: {
        width: 5,
        height: 5,
        borderRadius: "50%",
        background: "var(--sp-accent-mint)",
        flex: "none"
      }
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, a.contact)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, a.owner), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 13px/18px Roboto",
        color: a.status === "overdue" ? "#D93025" : a.status === "due" ? "#E8710A" : "var(--sp-text-muted)"
      }
    }, a.due), /*#__PURE__*/React.createElement(PillLabel, {
      tone: s.tone
    }, s.label));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", filtered.length, " of ", ACTIVITIES.length, " activities"));
}

// ────────────────────────────────────────────────────────────────────────
// Export
// ────────────────────────────────────────────────────────────────────────
Object.assign(window, {
  CONTACTS,
  ACTIVITIES,
  ACTIVITY_KIND,
  ACTIVITY_STATUS,
  ContactsList,
  ContactDetail,
  ActivitiesList,
  ActivityTimelineRow,
  ContactAvatar
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/contacts_activities.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/crm_companies.jsx
try { (() => {
// ─────────────────────────────────────────────────────────────────
// COMPANIES (a.k.a. Accounts)
//
// Aggregates everything the CRM knows about a company: contacts, subs,
// invoices, complaints, activities, NPS/health from CUSTOMER_PROFILES.
// Three states for each:
//   • "rich"      → has CUSTOMER_PROFILES entry → opens Customer 360
//   • "customer"  → in SUBS but no rich profile → opens Customer 360
//                                                  (falls back gracefully)
//   • "prospect"  → only in CONTACTS → opens lightweight Company detail
// ─────────────────────────────────────────────────────────────────

// Stable enrichment for prospects: industry/size/country are fixtures,
// not derived. Keyed by company name.
const COMPANY_ENRICH = {
  "Arcwell Robotics": {
    industry: "Robotics",
    country: "Denmark",
    size: "60 employees",
    domain: "arcwell.io",
    founded: 2019
  },
  "Beacon Logistics": {
    industry: "Logistics",
    country: "Spain",
    size: "180 employees",
    domain: "beaconlog.es",
    founded: 2014
  },
  "Vanguard Pensions": {
    industry: "Financial services",
    country: "Netherlands",
    size: "920 employees",
    domain: "vanguardpens.nl",
    founded: 1998
  },
  "Bauer & Söhne": {
    industry: "Manufacturing",
    country: "Germany",
    size: "240 employees",
    domain: "bauer-soehne.de",
    founded: 1962
  },
  "Pomme d'Or": {
    industry: "Hospitality",
    country: "France",
    size: "35 employees",
    domain: "pommedor.fr",
    founded: 2017
  },
  "Norrsken AB": {
    industry: "Clean energy",
    country: "Sweden",
    size: "110 employees",
    domain: "norrsken.se",
    founded: 2016
  },
  "Acme Holdings": {
    industry: "Conglomerate",
    country: "Netherlands",
    size: "1,400 employees",
    domain: "acme.com",
    founded: 1989
  },
  "Peregrine AI": {
    industry: "AI · ML platform",
    country: "USA",
    size: "45 employees",
    domain: "peregrine.ai",
    founded: 2022
  },
  "Hanzeborg NV": {
    industry: "Marine logistics",
    country: "Netherlands",
    size: "320 employees",
    domain: "hanzeborg.nl",
    founded: 2003
  },
  "Lumen Studios": {
    industry: "Creative agency",
    country: "Italy",
    size: "65 employees",
    domain: "lumenstudios.it",
    founded: 2011
  },
  "Meridian Fintech": {
    industry: "Fintech",
    country: "Finland",
    size: "560 employees",
    domain: "meridian.fi",
    founded: 2008
  },
  "Polder & Co": {
    industry: "Consultancy",
    country: "Netherlands",
    size: "12 employees",
    domain: "polder.co",
    founded: 2020
  },
  "Kairos Mobility": {
    industry: "Mobility · SaaS",
    country: "Portugal",
    size: "85 employees",
    domain: "kairos.pt",
    founded: 2018
  },
  "Thornebridge LLP": {
    industry: "Legal services",
    country: "UK",
    size: "210 employees",
    domain: "thornebridge.co.uk",
    founded: 1985
  }
};

// Build the unified company list from the multiple sources.
function _buildCompanies() {
  const map = new Map();
  const upsert = (name, patch) => {
    if (!name || name === "—") return;
    const cur = map.get(name) || {
      name
    };
    map.set(name, {
      ...cur,
      ...patch
    });
  };

  // 1. Rich profiles (highest priority)
  try {
    Object.values(CUSTOMER_PROFILES || {}).forEach(p => {
      upsert(p.name, {
        rich: true,
        customer: true,
        tier: p.tier,
        ownerAE: p.ownerAE,
        ownerCSM: p.ownerCSM,
        domain: p.domain,
        country: p.country,
        industry: p.industry,
        size: p.size,
        mrr: p.mrr,
        ltv: p.ltv,
        since: p.since,
        nps: p.nps,
        health: p.health,
        healthReason: p.healthReason,
        logoColor: p.logoColor
      });
    });
  } catch {}

  // 2. SUBS — companies with subscriptions
  try {
    (SUBS || []).forEach(s => {
      const cur = map.get(s.customer) || {};
      upsert(s.customer, {
        customer: true,
        ownerAE: cur.ownerAE || s.owner,
        mrr: (cur.mrr || 0) < s.mrr ? s.mrr : cur.mrr || s.mrr,
        since: cur.since || s.since,
        plan: s.plan
      });
    });
  } catch {}

  // 3. CONTACTS — sweep companies
  try {
    (CONTACTS || []).forEach(c => {
      if (!c.company || c.company === "—") return;
      upsert(c.company, {});
    });
  } catch {}

  // 4. Enrich
  Object.entries(COMPANY_ENRICH).forEach(([name, e]) => {
    if (map.has(name)) {
      const cur = map.get(name);
      map.set(name, {
        ...e,
        ...cur,
        ...{
          industry: cur.industry || e.industry,
          country: cur.country || e.country,
          size: cur.size || e.size,
          domain: cur.domain || e.domain,
          founded: cur.founded || e.founded
        }
      });
    }
  });
  return Array.from(map.values()).map(c => {
    const contacts = (CONTACTS || []).filter(ct => ct.company === c.name);
    const subs = (SUBS || []).filter(s => s.customer === c.name);
    const invs = (INVOICES || []).filter(i => i.customer === c.name);
    const open = invs.filter(i => i.status === "overdue" || i.status === "pending").length;
    const totalMrr = subs.reduce((sum, s) => sum + (s.status === "active" || s.status === "trialing" ? s.mrr : 0), 0);
    return {
      ...c,
      contacts: contacts.length,
      subs: subs.length,
      invoices: invs.length,
      openInvoices: open,
      mrr: c.mrr || totalMrr,
      tier: c.tier || subs[0]?.plan || "—",
      status: c.customer ? c.health === "red" ? "at-risk" : subs.some(s => s.status === "past_due") ? "past-due" : "active" : "prospect"
    };
  });
}
const COMPANIES = _buildCompanies();

// ─── Avatar ─────────────────────────────────────────────────────────────
function CompanyLogo({
  name,
  size = 36,
  color
}) {
  const initials = (name || "?").replace(/\s+(B\.V\.|GmbH|NV|Ltd|LLP|AB|Holdings|& Co|& Söhne)/, "").split(/\s+/).slice(0, 2).map(s => s[0] || "").join("").toUpperCase() || "?";
  const palette = ["#1A73E8", "var(--sp-accent-mint)", "var(--sp-accent-plum)", "var(--sp-accent-warm)", "#0EA5E9", "#EC4899", "#5F6368", "#E8710A"];
  let h = 0;
  for (let i = 0; i < name.length; i++) h = h * 31 + name.charCodeAt(i) >>> 0;
  const bg = color || palette[h % palette.length];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: size,
      height: size,
      borderRadius: 8,
      background: bg,
      display: "flex",
      alignItems: "center",
      justifyContent: "center",
      color: "#fff",
      font: `600 ${Math.round(size * 0.36)}px/1 Roboto`,
      letterSpacing: "0.02em",
      flexShrink: 0
    }
  }, initials);
}
function StatusPill({
  s
}) {
  const map = {
    "active": {
      bg: "rgba(0,184,148,.14)",
      fg: "var(--sp-accent-mint)",
      label: "Active customer"
    },
    "at-risk": {
      bg: "rgba(255,122,89,.16)",
      fg: "var(--sp-accent-warm)",
      label: "At-risk"
    },
    "past-due": {
      bg: "rgba(255,176,32,.18)",
      fg: "#B06000",
      label: "Past-due"
    },
    "prospect": {
      bg: "var(--sp-surface-2)",
      fg: "var(--sp-text-muted)",
      label: "Prospect"
    }
  };
  const v = map[s] || map.prospect;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      padding: "3px 10px",
      borderRadius: 999,
      background: v.bg,
      color: v.fg,
      font: "500 11px/14px Roboto",
      letterSpacing: "0.02em"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: v.fg
    }
  }), v.label);
}

// ─── Companies list ─────────────────────────────────────────────────────
function CompaniesList({
  onOpenCompany,
  onOpenCustomer
}) {
  const [q, setQ] = React.useState("");
  const [filter, setFilter] = React.useState("all");
  const [sort, setSort] = React.useState("mrr");
  const [view, setView] = React.useState("table"); // table | grid

  const filtered = COMPANIES.filter(c => {
    if (filter === "customers" && c.status === "prospect") return false;
    if (filter === "prospects" && c.status !== "prospect") return false;
    if (filter === "at-risk" && !(c.status === "at-risk" || c.status === "past-due")) return false;
    if (q && !(c.name.toLowerCase().includes(q.toLowerCase()) || (c.industry || "").toLowerCase().includes(q.toLowerCase()) || (c.country || "").toLowerCase().includes(q.toLowerCase()))) return false;
    return true;
  }).sort((a, b) => {
    if (sort === "mrr") return (b.mrr || 0) - (a.mrr || 0);
    if (sort === "name") return a.name.localeCompare(b.name);
    if (sort === "contacts") return b.contacts - a.contacts;
    return 0;
  });
  const counts = {
    all: COMPANIES.length,
    customers: COMPANIES.filter(c => c.status !== "prospect").length,
    prospects: COMPANIES.filter(c => c.status === "prospect").length,
    "at-risk": COMPANIES.filter(c => c.status === "at-risk" || c.status === "past-due").length
  };
  const totalMrr = COMPANIES.reduce((s, c) => s + (c.mrr || 0), 0);
  const open = c => {
    if (c.customer) onOpenCustomer && onOpenCustomer(c.name);else onOpenCompany && onOpenCompany(c);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page",
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap",
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Companies"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, COMPANIES.length, " accounts \xB7 \u20AC", (totalMrr / 100).toLocaleString("en", {
    maximumFractionDigits: 0
  }), "/mo aggregate MRR")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 13
      }
    }, "\u2193")
  }, "Export"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 13
      }
    }, "+")
  }, "New company"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement(KPITile, {
    label: "Total accounts",
    value: COMPANIES.length,
    delta: "+3 this month",
    tone: "muted"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "Active customers",
    value: counts.customers,
    delta: `${Math.round(counts.customers / COMPANIES.length * 100)}% of book`,
    tone: "mint"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "At-risk",
    value: counts["at-risk"],
    delta: "needs attention",
    tone: "warm"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "Pipeline",
    value: counts.prospects,
    delta: "open prospects",
    tone: "info"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    options: [{
      value: "all",
      label: `All · ${counts.all}`
    }, {
      value: "customers",
      label: `Customers · ${counts.customers}`
    }, {
      value: "prospects",
      label: `Prospects · ${counts.prospects}`
    }, {
      value: "at-risk",
      label: `At-risk · ${counts["at-risk"]}`
    }],
    value: filter,
    onChange: setFilter
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 200,
      maxWidth: 320
    }
  }, /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search by name, industry, country\u2026",
    value: q,
    onChange: setQ,
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-muted)"
      }
    }, "\u2315")
  })), /*#__PURE__*/React.createElement("select", {
    value: sort,
    onChange: e => setSort(e.target.value),
    style: {
      padding: "7px 10px",
      borderRadius: 6,
      border: "1px solid var(--sp-border)",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      font: "500 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement("option", {
    value: "mrr"
  }, "Sort: MRR \u2193"), /*#__PURE__*/React.createElement("option", {
    value: "name"
  }, "Sort: Name A\u2192Z"), /*#__PURE__*/React.createElement("option", {
    value: "contacts"
  }, "Sort: Contacts \u2193")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      overflow: "hidden"
    }
  }, ["table", "grid"].map(v => /*#__PURE__*/React.createElement("button", {
    key: v,
    onClick: () => setView(v),
    style: {
      padding: "6px 12px",
      border: "none",
      background: view === v ? "var(--sp-surface-2)" : "transparent",
      color: view === v ? "var(--sp-text)" : "var(--sp-text-muted)",
      font: "500 12px/16px Roboto",
      cursor: "pointer",
      textTransform: "capitalize"
    }
  }, v)))), view === "table" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "minmax(220px, 1.6fr) 1fr 1fr 100px 100px 130px 120px 28px",
      padding: "12px 18px",
      borderBottom: "1px solid var(--sp-border)",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Company"), /*#__PURE__*/React.createElement("div", null, "Industry"), /*#__PURE__*/React.createElement("div", null, "Owner (AE)"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, "Contacts"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, "Subs"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, "MRR"), /*#__PURE__*/React.createElement("div", null, "Status"), /*#__PURE__*/React.createElement("div", null)), filtered.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.name,
    onClick: () => open(c),
    style: {
      display: "grid",
      gridTemplateColumns: "minmax(220px, 1.6fr) 1fr 1fr 100px 100px 130px 120px 28px",
      padding: "12px 18px",
      borderBottom: "1px solid var(--sp-border)",
      alignItems: "center",
      cursor: "pointer",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement(CompanyLogo, {
    name: c.name,
    color: c.logoColor
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, c.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.domain || "—", " \xB7 ", c.country || "—"))), /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--sp-text-muted)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, c.industry || "—"), /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--sp-text)"
    }
  }, c.ownerAE || "—"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right",
      color: "var(--sp-text)"
    }
  }, c.contacts), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right",
      color: "var(--sp-text)"
    }
  }, c.subs || "—"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right",
      color: "var(--sp-text)",
      font: "500 13px/18px Roboto"
    }
  }, c.mrr ? `€${(c.mrr / 100).toLocaleString("en", {
    maximumFractionDigits: 0
  })}` : "—"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(StatusPill, {
    s: c.status
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--sp-text-muted)",
      textAlign: "right"
    }
  }, "\u203A"))), filtered.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "No companies match your filters.")), view === "grid" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fill, minmax(280px, 1fr))",
      gap: 12
    }
  }, filtered.map(c => /*#__PURE__*/React.createElement(PCard, {
    key: c.name,
    hover: true
  }, /*#__PURE__*/React.createElement("div", {
    onClick: () => open(c),
    style: {
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 12,
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement(CompanyLogo, {
    name: c.name,
    color: c.logoColor,
    size: 44
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0,
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/19px Roboto",
      color: "var(--sp-text)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, c.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, c.industry || "—"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 10
    }
  }, /*#__PURE__*/React.createElement(StatusPill, {
    s: c.status
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, c.mrr ? `€${(c.mrr / 100).toLocaleString("en", {
    maximumFractionDigits: 0
  })}/mo` : "—")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1fr",
      gap: 8,
      paddingTop: 10,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(MiniStat, {
    label: "Contacts",
    value: c.contacts
  }), /*#__PURE__*/React.createElement(MiniStat, {
    label: "Subs",
    value: c.subs || "—"
  }), /*#__PURE__*/React.createElement(MiniStat, {
    label: "Open inv",
    value: c.openInvoices || "—",
    tone: c.openInvoices ? "warm" : "muted"
  })))))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", filtered.length, " of ", COMPANIES.length, " companies"));
}
function KPITile({
  label,
  value,
  delta,
  tone = "muted"
}) {
  const colors = {
    mint: "var(--sp-accent-mint)",
    warm: "var(--sp-accent-warm)",
    info: "#1A73E8",
    muted: "var(--sp-text-muted)"
  }[tone];
  return /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 6
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 28px/32px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.01em"
    }
  }, value), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: colors,
      marginTop: 4
    }
  }, delta));
}
function MiniStat({
  label,
  value,
  tone
}) {
  const fg = tone === "warm" ? "var(--sp-accent-warm)" : "var(--sp-text)";
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 10px/13px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.05em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/18px Roboto",
      color: fg,
      marginTop: 2
    }
  }, value));
}

// ─── Company detail (prospects only — customers go to Customer 360) ─────
function CompanyDetail({
  company,
  onBack,
  onOpenContact
}) {
  if (!company) {
    return /*#__PURE__*/React.createElement("div", {
      className: "sp-page",
      style: {
        padding: 24
      }
    }, /*#__PURE__*/React.createElement("span", {
      onClick: onBack,
      style: {
        cursor: "pointer",
        font: "500 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "\u2190 Back to companies"), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 24,
        padding: 48,
        textAlign: "center",
        color: "var(--sp-text-muted)"
      }
    }, "Company not found."));
  }
  const contacts = (CONTACTS || []).filter(c => c.company === company.name);
  const activities = (ACTIVITIES || []).filter(a => a.company === company.name).slice(0, 8);
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page",
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back to companies"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement(CompanyLogo, {
    name: company.name,
    color: company.logoColor,
    size: 64
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 26px/32px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, company.name), /*#__PURE__*/React.createElement(StatusPill, {
    s: company.status
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, company.industry || "—", " \xB7 ", company.country || "—", " \xB7 ", company.size || "—", " ", company.founded ? `· founded ${company.founded}` : ""), company.domain && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "#1A73E8",
      marginTop: 6
    }
  }, company.domain)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", null, "\u260F")
  }, "Log call"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", null, "\u2709")
  }, "Email"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 13
      }
    }, "+")
  }, "Create deal")))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 16,
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: 0,
      color: "var(--sp-text)"
    }
  }, "Contacts ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto"
    }
  }, "(", contacts.length, ")")), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "+ Add contact")), contacts.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto",
      borderRadius: 8,
      background: "var(--sp-surface-2)"
    }
  }, "No contacts at this company yet.") : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, contacts.map(ct => /*#__PURE__*/React.createElement("div", {
    key: ct.id,
    onClick: () => onOpenContact && onOpenContact(ct),
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "10px 6px",
      borderBottom: "1px solid var(--sp-border)",
      cursor: "pointer",
      borderRadius: 6
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement(ContactAvatar, {
    name: ct.name,
    size: 32
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, ct.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, ct.role, " \xB7 ", ct.email)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, ct.last))))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: 0,
      color: "var(--sp-text)"
    }
  }, "Recent activity"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "+ Log activity")), activities.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto",
      borderRadius: 8,
      background: "var(--sp-surface-2)"
    }
  }, "No activity logged yet.") : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, activities.map(a => {
    const k = ACTIVITY_KIND[a.kind] || ACTIVITY_KIND.note;
    const s = ACTIVITY_STATUS[a.status] || ACTIVITY_STATUS.done;
    return /*#__PURE__*/React.createElement("div", {
      key: a.id,
      style: {
        display: "flex",
        gap: 12,
        padding: "10px 0",
        borderBottom: "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 28,
        height: 28,
        borderRadius: 6,
        background: k.color + "1F",
        color: k.color,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        font: "500 14px/1 Roboto",
        flexShrink: 0
      }
    }, k.glyph), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, a.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, a.contact, " \xB7 ", a.owner, " \xB7 ", a.due)), /*#__PURE__*/React.createElement(PBadge, {
      variant: s.tone
    }, s.label));
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 12px",
      color: "var(--sp-text)"
    }
  }, "About"), /*#__PURE__*/React.createElement(KV2, {
    label: "Industry",
    value: company.industry || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Country",
    value: company.country || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Size",
    value: company.size || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Domain",
    value: company.domain || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Founded",
    value: company.founded || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Owner (AE)",
    value: company.ownerAE || "—"
  })), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 12px",
      color: "var(--sp-text)"
    }
  }, "Pipeline signals"), /*#__PURE__*/React.createElement(KV2, {
    label: "Status",
    value: /*#__PURE__*/React.createElement(StatusPill, {
      s: company.status
    })
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Deals open",
    value: "\u2014"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Last activity",
    value: activities[0]?.due || "—"
  }), /*#__PURE__*/React.createElement(KV2, {
    label: "Next activity",
    value: activities.find(a => a.status === "due" || a.status === "scheduled")?.due || "—"
  })))));
}
function KV2({
  label,
  value
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "6px 0",
      borderBottom: "1px dashed var(--sp-border)",
      font: "400 13px/18px Roboto"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)",
      fontWeight: 500
    }
  }, value));
}

// ─────────────────────────────────────────────────────────────────
// EXPORTS
// ─────────────────────────────────────────────────────────────────
Object.assign(window, {
  COMPANIES,
  COMPANY_ENRICH,
  CompanyLogo,
  CompaniesList,
  CompanyDetail
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/crm_companies.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/crm_deals.jsx
try { (() => {
// ─────────────────────────────────────────────────────────────────
// DEALS — sales pipeline (kanban) + deal detail
//
// A "deal" is a sales opportunity tied to a company + primary contact.
// Stages: lead → qualified → proposal → negotiation → won / lost
// Wired off COMPANIES + CONTACTS so navigation is consistent.
// ─────────────────────────────────────────────────────────────────

const DEAL_STAGES = [{
  id: "lead",
  label: "Lead",
  color: "#5F6368",
  prob: 10
}, {
  id: "qualified",
  label: "Qualified",
  color: "#1A73E8",
  prob: 30
}, {
  id: "proposal",
  label: "Proposal",
  color: "#673AB7",
  prob: 55
}, {
  id: "negotiation",
  label: "Negotiation",
  color: "#E8710A",
  prob: 75
}, {
  id: "won",
  label: "Won",
  color: "var(--sp-accent-mint)",
  prob: 100
}, {
  id: "lost",
  label: "Lost",
  color: "var(--sp-text-muted)",
  prob: 0
}];
const DEAL_STAGE_BY_ID = Object.fromEntries(DEAL_STAGES.map(s => [s.id, s]));
const DEALS = [
// ── Customer expansions
{
  id: "deal-001",
  name: "Orbit Labs · Enterprise upgrade",
  company: "Orbit Labs B.V.",
  contact: "Elin Karlsson",
  owner: "Bram de Vries",
  stage: "negotiation",
  value: 8400000,
  closeDate: "Nov 15",
  source: "expansion",
  created: "Sep 02",
  lastActivity: "Today",
  priority: "high",
  tags: ["expansion", "enterprise-tier"],
  note: "Pricing reviewed by Finance, awaiting CFO sign-off."
}, {
  id: "deal-002",
  name: "Meridian · Renewal + 60 seats",
  company: "Meridian Fintech",
  contact: "Amira Haddad",
  owner: "Priya Shah",
  stage: "proposal",
  value: 12480000,
  closeDate: "Nov 30",
  source: "renewal",
  created: "Sep 14",
  lastActivity: "Yesterday",
  priority: "high",
  tags: ["renewal", "expansion"],
  note: "Multi-year discount on the table; legal review next week."
}, {
  id: "deal-003",
  name: "Hanzeborg · 12-month commit",
  company: "Hanzeborg NV",
  contact: "Femke de Wit",
  owner: "Bram de Vries",
  stage: "qualified",
  value: 2016000,
  closeDate: "Dec 05",
  source: "renewal",
  created: "Oct 02",
  lastActivity: "Oct 17",
  priority: "medium",
  tags: ["renewal"],
  note: "Champion confirmed budget; needs SSO add-on quote."
}, {
  id: "deal-004",
  name: "Kairos · Geo expansion (PT→ES)",
  company: "Kairos Mobility",
  contact: "Rafael Duarte",
  owner: "Chiara Romano",
  stage: "lead",
  value: 1080000,
  closeDate: "Jan 20",
  source: "outbound",
  created: "Oct 14",
  lastActivity: "Oct 16",
  priority: "low",
  tags: ["expansion"],
  note: "Discovery call booked for next Tuesday."
},
// ── Pure prospects
{
  id: "deal-100",
  name: "Vanguard Pensions · Pilot",
  company: "Vanguard Pensions",
  contact: "Thomas Verwey",
  owner: "Anna Krause",
  stage: "qualified",
  value: 4500000,
  closeDate: "Dec 15",
  source: "inbound",
  created: "Sep 28",
  lastActivity: "Apr 14",
  priority: "high",
  tags: ["enterprise", "security-review"],
  note: "Security review in flight; SOC2 + DPIA shared."
}, {
  id: "deal-101",
  name: "Arcwell Robotics · Growth",
  company: "Arcwell Robotics",
  contact: "Marlon Sørensen",
  owner: "Bram de Vries",
  stage: "proposal",
  value: 1980000,
  closeDate: "Nov 22",
  source: "referral",
  created: "Sep 30",
  lastActivity: "Today",
  priority: "medium",
  tags: ["referral"],
  note: "Warm referral from Elin Karlsson at Orbit Labs."
}, {
  id: "deal-102",
  name: "Beacon Logistics · Pilot → Growth",
  company: "Beacon Logistics",
  contact: "Irene Costa",
  owner: "Chiara Romano",
  stage: "negotiation",
  value: 1188000,
  closeDate: "Nov 08",
  source: "inbound",
  created: "Aug 12",
  lastActivity: "Apr 16",
  priority: "high",
  tags: ["pilot", "conversion"],
  note: "Pilot success; converting to annual Growth."
}, {
  id: "deal-103",
  name: "Bauer & Söhne · Starter",
  company: "Bauer & Söhne",
  contact: "Greta Ostermann",
  owner: "Anna Krause",
  stage: "qualified",
  value: 588000,
  closeDate: "Dec 28",
  source: "outbound",
  created: "Sep 18",
  lastActivity: "Oct 16",
  priority: "low",
  tags: ["smb", "dach"],
  note: "Procurement reviewing MSA."
}, {
  id: "deal-104",
  name: "Pomme d'Or · Starter",
  company: "Pomme d'Or",
  contact: "Sophie Laurent",
  owner: "Bram de Vries",
  stage: "lead",
  value: 294000,
  closeDate: "Jan 10",
  source: "inbound",
  created: "Mar 18",
  lastActivity: "Mar 22",
  priority: "low",
  tags: ["smb"],
  note: "Co-founder requested intro; no firm timeline yet."
}, {
  id: "deal-105",
  name: "Norrsken AB · Carbon-tracking add-on",
  company: "Norrsken AB",
  contact: "Ola Lindqvist",
  owner: "Anna Krause",
  stage: "lead",
  value: 720000,
  closeDate: "Feb 14",
  source: "outbound",
  created: "Oct 07",
  lastActivity: "Sep 14",
  priority: "low",
  tags: ["new-product"],
  note: "Pre-seed for an unreleased module; track interest."
},
// ── Closed
{
  id: "deal-200",
  name: "Lumen Studios · Q3 renewal",
  company: "Lumen Studios",
  contact: "Julia Marchetti",
  owner: "Chiara Romano",
  stage: "won",
  value: 1368000,
  closeDate: "Sep 30",
  source: "renewal",
  created: "Aug 15",
  lastActivity: "Sep 30",
  priority: "medium",
  tags: ["renewal", "csat-9"],
  note: "Closed-won. CSAT 9/10."
}, {
  id: "deal-201",
  name: "Acme · Bot license expansion",
  company: "Acme Holdings",
  contact: "Ruben Jansen",
  owner: "Anna Krause",
  stage: "won",
  value: 4200000,
  closeDate: "Aug 22",
  source: "expansion",
  created: "Jul 04",
  lastActivity: "Aug 22",
  priority: "high",
  tags: ["expansion"],
  note: "Closed-won; provisioned."
}, {
  id: "deal-202",
  name: "Polder & Co · Growth migration",
  company: "Polder & Co",
  contact: "Dieter Polder",
  owner: "Bram de Vries",
  stage: "lost",
  value: 252000,
  closeDate: "Sep 12",
  source: "outbound",
  created: "Jul 20",
  lastActivity: "Sep 12",
  priority: "low",
  tags: ["lost-to-competitor"],
  note: "Lost to a cheaper local incumbent."
}, {
  id: "deal-203",
  name: "Peregrine AI · Trial conversion",
  company: "Peregrine AI",
  contact: "Victor Huang",
  owner: "Chiara Romano",
  stage: "qualified",
  value: 588000,
  closeDate: "Nov 30",
  source: "trial",
  created: "Oct 08",
  lastActivity: "Oct 14",
  priority: "medium",
  tags: ["trial"],
  note: "Trial usage strong; conversion call scheduled."
}];

// ─── Helpers ────────────────────────────────────────────────────────────
function _money(cents) {
  if (cents == null) return "—";
  if (cents >= 100000000) return `€${(cents / 100000000).toFixed(1)}M`;
  if (cents >= 100000) return `€${Math.round(cents / 100000)}k`;
  return `€${(cents / 100).toLocaleString("en", {
    maximumFractionDigits: 0
  })}`;
}
function _weighted(d) {
  return Math.round(d.value * (DEAL_STAGE_BY_ID[d.stage]?.prob || 0) / 100);
}

// ─── Stage chip ─────────────────────────────────────────────────────────
function StageChip({
  stage
}) {
  const s = DEAL_STAGE_BY_ID[stage] || DEAL_STAGES[0];
  const color = s.color;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      padding: "3px 10px",
      borderRadius: 999,
      background: `color-mix(in srgb, ${color} 14%, transparent)`,
      color: color,
      font: "500 11px/14px Roboto",
      letterSpacing: "0.02em"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: color
    }
  }), s.label);
}
function PriorityDot({
  p
}) {
  const c = {
    high: "var(--sp-accent-warm)",
    medium: "#E8710A",
    low: "var(--sp-text-muted)"
  }[p] || "var(--sp-text-muted)";
  return /*#__PURE__*/React.createElement("span", {
    title: p,
    style: {
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: c,
      display: "inline-block"
    }
  });
}

// ─── Pipeline (kanban) view ─────────────────────────────────────────────
function DealsKanban({
  deals,
  onOpen
}) {
  // Group
  const byStage = Object.fromEntries(DEAL_STAGES.map(s => [s.id, []]));
  deals.forEach(d => {
    if (byStage[d.stage]) byStage[d.stage].push(d);
  });
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 12,
      overflowX: "auto",
      paddingBottom: 8
    }
  }, DEAL_STAGES.map(s => {
    const list = byStage[s.id];
    const total = list.reduce((sum, d) => sum + d.value, 0);
    return /*#__PURE__*/React.createElement("div", {
      key: s.id,
      style: {
        minWidth: 280,
        width: 280,
        flexShrink: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        padding: "8px 12px",
        borderRadius: "8px 8px 0 0",
        background: `color-mix(in srgb, ${s.color} 8%, var(--sp-surface))`,
        borderLeft: `3px solid ${s.color}`,
        borderTop: "1px solid var(--sp-border)",
        borderRight: "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "600 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, s.label), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        padding: "1px 7px",
        borderRadius: 999,
        background: "var(--sp-surface-2)"
      }
    }, list.length)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, _money(total))), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: 8,
        minHeight: 360,
        background: "var(--sp-surface-2)",
        borderRadius: "0 0 8px 8px",
        border: "1px solid var(--sp-border)",
        borderTop: "none",
        display: "flex",
        flexDirection: "column",
        gap: 8
      }
    }, list.length === 0 && /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        textAlign: "center",
        padding: "20px 0",
        border: "1px dashed var(--sp-border)",
        borderRadius: 6
      }
    }, "No deals"), list.map(d => /*#__PURE__*/React.createElement("div", {
      key: d.id,
      onClick: () => onOpen(d),
      style: {
        background: "var(--sp-surface)",
        borderRadius: 6,
        border: "1px solid var(--sp-border)",
        padding: 12,
        cursor: "pointer",
        transition: "transform .12s, box-shadow .12s"
      },
      onMouseEnter: e => {
        e.currentTarget.style.transform = "translateY(-1px)";
        e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,.08)";
      },
      onMouseLeave: e => {
        e.currentTarget.style.transform = "translateY(0)";
        e.currentTarget.style.boxShadow = "none";
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "flex-start",
        gap: 8,
        marginBottom: 6
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)",
        flex: 1,
        minWidth: 0
      }
    }, d.name), /*#__PURE__*/React.createElement(PriorityDot, {
      p: d.priority
    })), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginBottom: 8
      }
    }, d.company), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "600 14px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, _money(d.value)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, d.closeDate)), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        marginTop: 8,
        paddingTop: 8,
        borderTop: "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 6
      }
    }, /*#__PURE__*/React.createElement(ContactAvatar, {
      name: d.owner,
      size: 22
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, d.owner.split(" ")[0])), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, d.lastActivity))))));
  }));
}

// ─── Deals list view (table) ────────────────────────────────────────────
function DealsTable({
  deals,
  onOpen
}) {
  return /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "minmax(220px, 1.6fr) minmax(160px, 1fr) 130px 120px 100px 100px 100px 28px",
      padding: "12px 18px",
      borderBottom: "1px solid var(--sp-border)",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, /*#__PURE__*/React.createElement("div", null, "Deal"), /*#__PURE__*/React.createElement("div", null, "Company"), /*#__PURE__*/React.createElement("div", null, "Stage"), /*#__PURE__*/React.createElement("div", null, "Owner"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, "Value"), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, "Weighted"), /*#__PURE__*/React.createElement("div", null, "Close"), /*#__PURE__*/React.createElement("div", null)), deals.map(d => /*#__PURE__*/React.createElement("div", {
    key: d.id,
    onClick: () => onOpen(d),
    style: {
      display: "grid",
      gridTemplateColumns: "minmax(220px, 1.6fr) minmax(160px, 1fr) 130px 120px 100px 100px 100px 28px",
      padding: "12px 18px",
      borderBottom: "1px solid var(--sp-border)",
      alignItems: "center",
      cursor: "pointer",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement(PriorityDot, {
    p: d.priority
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, d.name), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, d.contact))), /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--sp-text-muted)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, d.company), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(StageChip, {
    stage: d.stage
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(ContactAvatar, {
    name: d.owner,
    size: 22
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text)",
      overflow: "hidden",
      textOverflow: "ellipsis",
      whiteSpace: "nowrap"
    }
  }, d.owner.split(" ")[0])), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, _money(d.value)), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, _money(_weighted(d))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, d.closeDate), /*#__PURE__*/React.createElement("div", {
    style: {
      color: "var(--sp-text-muted)",
      textAlign: "right"
    }
  }, "\u203A"))), deals.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 48,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 14px/20px Roboto"
    }
  }, "No deals match your filters."));
}

// ─── Deals page (top-level) ─────────────────────────────────────────────
function DealsPage({
  onOpenDeal,
  onOpenCompany
}) {
  const [view, setView] = React.useState("kanban"); // kanban | table
  const [owner, setOwner] = React.useState("all");
  const [q, setQ] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("open"); // open | all | won | lost

  const owners = ["all", ...Array.from(new Set(DEALS.map(d => d.owner)))];
  const filtered = DEALS.filter(d => {
    if (statusFilter === "open" && (d.stage === "won" || d.stage === "lost")) return false;
    if (statusFilter === "won" && d.stage !== "won") return false;
    if (statusFilter === "lost" && d.stage !== "lost") return false;
    if (owner !== "all" && d.owner !== owner) return false;
    if (q && !(d.name.toLowerCase().includes(q.toLowerCase()) || d.company.toLowerCase().includes(q.toLowerCase()) || d.contact.toLowerCase().includes(q.toLowerCase()))) return false;
    return true;
  });
  const open = filtered.filter(d => d.stage !== "won" && d.stage !== "lost");
  const total = open.reduce((s, d) => s + d.value, 0);
  const weighted = open.reduce((s, d) => s + _weighted(d), 0);
  const wonValue = DEALS.filter(d => d.stage === "won").reduce((s, d) => s + d.value, 0);
  const winRate = (() => {
    const closed = DEALS.filter(d => d.stage === "won" || d.stage === "lost").length;
    if (!closed) return 0;
    return Math.round(DEALS.filter(d => d.stage === "won").length / closed * 100);
  })();
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page",
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "space-between",
      gap: 20,
      flexWrap: "wrap",
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, "Deals"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, open.length, " open \xB7 ", DEALS.filter(d => d.stage === "won").length, " won this quarter")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 13
      }
    }, "\u2193")
  }, "Export"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        fontSize: 13
      }
    }, "+")
  }, "New deal"))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement(KPITile, {
    label: "Open pipeline",
    value: _money(total),
    delta: `${open.length} deals`,
    tone: "info"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "Weighted forecast",
    value: _money(weighted),
    delta: "prob \xD7 value",
    tone: "mint"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "Won this quarter",
    value: _money(wonValue),
    delta: `${DEALS.filter(d => d.stage === "won").length} closed`,
    tone: "mint"
  }), /*#__PURE__*/React.createElement(KPITile, {
    label: "Win rate",
    value: `${winRate}%`,
    delta: "last 90 days",
    tone: "muted"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    options: [{
      value: "open",
      label: `Open · ${DEALS.filter(d => d.stage !== "won" && d.stage !== "lost").length}`
    }, {
      value: "won",
      label: `Won · ${DEALS.filter(d => d.stage === "won").length}`
    }, {
      value: "lost",
      label: `Lost · ${DEALS.filter(d => d.stage === "lost").length}`
    }, {
      value: "all",
      label: `All · ${DEALS.length}`
    }],
    value: statusFilter,
    onChange: setStatusFilter
  }), /*#__PURE__*/React.createElement("select", {
    value: owner,
    onChange: e => setOwner(e.target.value),
    style: {
      padding: "7px 10px",
      borderRadius: 6,
      border: "1px solid var(--sp-border)",
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      font: "500 12px/16px Roboto"
    }
  }, owners.map(o => /*#__PURE__*/React.createElement("option", {
    key: o,
    value: o
  }, o === "all" ? "All owners" : o))), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 200,
      maxWidth: 320
    }
  }, /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search deals, companies, contacts\u2026",
    value: q,
    onChange: setQ,
    leading: /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-muted)"
      }
    }, "\u2315")
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      overflow: "hidden"
    }
  }, [{
    v: "kanban",
    l: "Pipeline"
  }, {
    v: "table",
    l: "Table"
  }].map(({
    v,
    l
  }) => /*#__PURE__*/React.createElement("button", {
    key: v,
    onClick: () => setView(v),
    style: {
      padding: "6px 12px",
      border: "none",
      background: view === v ? "var(--sp-surface-2)" : "transparent",
      color: view === v ? "var(--sp-text)" : "var(--sp-text-muted)",
      font: "500 12px/16px Roboto",
      cursor: "pointer"
    }
  }, l)))), view === "kanban" ? /*#__PURE__*/React.createElement(DealsKanban, {
    deals: filtered,
    onOpen: onOpenDeal
  }) : /*#__PURE__*/React.createElement(DealsTable, {
    deals: filtered,
    onOpen: onOpenDeal
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      marginTop: 14
    }
  }, "Showing ", filtered.length, " of ", DEALS.length, " deals"));
}

// ─── Deal detail ────────────────────────────────────────────────────────
function DealDetail({
  deal,
  onBack,
  onOpenCompany,
  onOpenContact
}) {
  if (!deal) {
    return /*#__PURE__*/React.createElement("div", {
      className: "sp-page",
      style: {
        padding: 24
      }
    }, /*#__PURE__*/React.createElement("span", {
      onClick: onBack,
      style: {
        cursor: "pointer",
        font: "500 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "\u2190 Back to deals"), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 24,
        padding: 48,
        textAlign: "center",
        color: "var(--sp-text-muted)"
      }
    }, "Deal not found."));
  }
  const stage = DEAL_STAGE_BY_ID[deal.stage];
  const idx = DEAL_STAGES.findIndex(s => s.id === deal.stage);
  const contacts = (CONTACTS || []).filter(c => c.company === deal.company);
  const activities = (ACTIVITIES || []).filter(a => a.company === deal.company).slice(0, 6);
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page",
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12,
      display: "inline-block"
    }
  }, "\u2190 Back to deals"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      margin: 0,
      color: "var(--sp-text)",
      letterSpacing: "-0.015em"
    }
  }, deal.name), /*#__PURE__*/React.createElement(StageChip, {
    stage: deal.stage
  }), /*#__PURE__*/React.createElement(PriorityDot, {
    p: deal.priority
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, deal.priority, " priority")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 24,
      marginTop: 14,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(Stat2, {
    label: "Value",
    value: _money(deal.value)
  }), /*#__PURE__*/React.createElement(Stat2, {
    label: "Weighted",
    value: _money(_weighted(deal)),
    sub: `${stage.prob}% probability`
  }), /*#__PURE__*/React.createElement(Stat2, {
    label: "Expected close",
    value: deal.closeDate
  }), /*#__PURE__*/React.createElement(Stat2, {
    label: "Source",
    value: deal.source
  }), /*#__PURE__*/React.createElement(Stat2, {
    label: "Created",
    value: deal.created
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      flexShrink: 0
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", null, "\u260F")
  }, "Log call"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", null, "\u2709")
  }, "Email"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement("span", null, "\u2713")
  }, "Mark won"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      display: "flex",
      gap: 4
    }
  }, DEAL_STAGES.filter(s => s.id !== "lost").map((s, i) => {
    const reached = i <= idx && deal.stage !== "lost";
    return /*#__PURE__*/React.createElement("div", {
      key: s.id,
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        height: 4,
        borderRadius: 2,
        background: reached ? s.color : "var(--sp-surface-2)"
      }
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: reached ? "var(--sp-text)" : "var(--sp-text-muted)",
        marginTop: 6,
        textAlign: "left"
      }
    }, s.label));
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 16,
      marginTop: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 10px",
      color: "var(--sp-text)"
    }
  }, "Notes"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text)",
      padding: 14,
      background: "var(--sp-surface-2)",
      borderRadius: 8,
      borderLeft: `3px solid ${stage.color}`
    }
  }, deal.note), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement("textarea", {
    placeholder: "Add a note\u2026",
    style: {
      width: "100%",
      minHeight: 60,
      resize: "vertical",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      padding: 10,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: 0,
      color: "var(--sp-text)"
    }
  }, "Activity at ", deal.company), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "+ Log")), activities.length === 0 ? /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto",
      borderRadius: 8,
      background: "var(--sp-surface-2)"
    }
  }, "No activity logged yet.") : /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column"
    }
  }, activities.map(a => {
    const k = ACTIVITY_KIND[a.kind] || ACTIVITY_KIND.note;
    const s = ACTIVITY_STATUS[a.status] || ACTIVITY_STATUS.done;
    return /*#__PURE__*/React.createElement("div", {
      key: a.id,
      style: {
        display: "flex",
        gap: 12,
        padding: "10px 0",
        borderBottom: "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        width: 28,
        height: 28,
        borderRadius: 6,
        background: k.color + "1F",
        color: k.color,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        font: "500 14px/1 Roboto",
        flexShrink: 0
      }
    }, k.glyph), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, a.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, a.contact, " \xB7 ", a.owner, " \xB7 ", a.due)), /*#__PURE__*/React.createElement(PBadge, {
      variant: s.tone
    }, s.label));
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 10px",
      color: "var(--sp-text)"
    }
  }, "Tags"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      flexWrap: "wrap"
    }
  }, deal.tags.map(t => /*#__PURE__*/React.createElement("span", {
    key: t,
    style: {
      padding: "3px 10px",
      borderRadius: 999,
      background: "var(--sp-surface-2)",
      color: "var(--sp-text-muted)",
      font: "500 11px/14px Roboto",
      border: "1px solid var(--sp-border)"
    }
  }, "#", t))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 12px",
      color: "var(--sp-text)"
    }
  }, "Account"), /*#__PURE__*/React.createElement("div", {
    onClick: () => onOpenCompany && onOpenCompany(deal.company),
    style: {
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: 8,
      borderRadius: 6,
      background: "var(--sp-surface-2)"
    }
  }, /*#__PURE__*/React.createElement(CompanyLogo, {
    name: deal.company,
    size: 36
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, deal.company), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "#1A73E8"
    }
  }, "View company \u2192")))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 12px",
      color: "var(--sp-text)"
    }
  }, "Primary contact"), (() => {
    const c = (CONTACTS || []).find(x => x.name === deal.contact);
    if (!c) return /*#__PURE__*/React.createElement("div", {
      style: {
        color: "var(--sp-text-muted)",
        font: "400 13px/18px Roboto"
      }
    }, deal.contact);
    return /*#__PURE__*/React.createElement("div", {
      onClick: () => onOpenContact && onOpenContact(c),
      style: {
        cursor: "pointer",
        display: "flex",
        alignItems: "center",
        gap: 10
      }
    }, /*#__PURE__*/React.createElement(ContactAvatar, {
      name: c.name,
      size: 36
    }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, c.name), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, c.role, " \xB7 ", c.email)));
  })(), contacts.length > 1 && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      paddingTop: 12,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginBottom: 8
    }
  }, "Other stakeholders (", contacts.length - 1, ")"), contacts.filter(c => c.name !== deal.contact).map(c => /*#__PURE__*/React.createElement("div", {
    key: c.id,
    onClick: () => onOpenContact && onOpenContact(c),
    style: {
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      gap: 8,
      padding: "4px 0",
      font: "400 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement(ContactAvatar, {
    name: c.name,
    size: 22
  }), /*#__PURE__*/React.createElement("span", null, c.name), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, "\xB7 ", c.role))))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 15px/20px Roboto",
      margin: "0 0 12px",
      color: "var(--sp-text)"
    }
  }, "Owner"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(ContactAvatar, {
    name: deal.owner,
    size: 36
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, deal.owner), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Account executive")))))));
}
function Stat2({
  label,
  value,
  sub
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 18px/24px Roboto",
      color: "var(--sp-text)",
      marginTop: 2
    }
  }, value), sub && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, sub));
}

// ─────────────────────────────────────────────────────────────────
Object.assign(window, {
  DEALS,
  DEAL_STAGES,
  DEAL_STAGE_BY_ID,
  DealsPage,
  DealDetail,
  DealsKanban,
  DealsTable,
  StageChip
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/crm_deals.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/portal_Extra.jsx
try { (() => {
// Portal — Products & Tickets screens
// Products: what the customer is using + available add-ons catalog
// Tickets: support conversations (list + threaded detail)

// ── Fixtures ────────────────────────────────────────────────────────────
const MY_PRODUCTS = [{
  id: "p_core",
  name: "CRM Core",
  desc: "Contacts, deals, pipelines, activities.",
  included: true,
  status: "active",
  seats: 42,
  usage: 92,
  icon: "◆",
  color: "#1A73E8"
}, {
  id: "p_auto",
  name: "Automation",
  desc: "Workflows, triggers, scheduled tasks.",
  included: true,
  status: "active",
  runs: 1284,
  cap: 2000,
  icon: "⟳",
  color: "var(--sp-accent-mint)"
}, {
  id: "p_api",
  name: "API Access",
  desc: "REST + webhooks · 10k req/day included.",
  included: true,
  status: "active",
  reqs: 6420,
  cap: 10000,
  icon: "⧉",
  color: "var(--sp-accent-plum)"
}, {
  id: "p_reports",
  name: "Advanced Reports",
  desc: "Custom dashboards, saved views, CSV exports.",
  included: false,
  status: "addon",
  price: 4900,
  since: "May 2025",
  icon: "▤",
  color: "#F9A825"
}, {
  id: "p_deskphones",
  name: "Desk phones (6x Yealink T54W)",
  desc: "Bundled VoIP hardware for dialer users.",
  included: false,
  status: "addon",
  price: 9900,
  since: "Aug 2025",
  icon: "☎",
  color: "#546E7A",
  physical: true,
  shipsTo: "ad2"
}];
const PRODUCT_CATALOG = [{
  id: "c_ai",
  name: "AI Assistant",
  desc: "Auto-draft follow-ups, summarise calls, suggest next steps.",
  price: 2900,
  tag: "New",
  color: "var(--sp-accent-plum)"
}, {
  id: "c_insights",
  name: "Revenue Insights",
  desc: "Forecasting, win/loss analytics, cohort breakdowns.",
  price: 6900,
  tag: "Popular",
  color: "var(--sp-accent-mint)"
}, {
  id: "c_dialer",
  name: "Sales Dialer",
  desc: "In-app calling, voicemail drop, call recording + transcripts.",
  price: 3900,
  tag: null,
  color: "#1A73E8"
}, {
  id: "c_enrich",
  name: "Data Enrichment",
  desc: "Auto-fill contacts from public sources. 500 credits / mo.",
  price: 1900,
  tag: null,
  color: "#F9A825"
}, {
  id: "c_welcome",
  name: "Onboarding Welcome Kit",
  desc: "Printed playbook, stickers, and branded notebooks shipped to your team.",
  price: 12900,
  tag: null,
  color: "#546E7A",
  physical: true,
  oneTime: true
}];
const TICKETS = [{
  id: "T-2048",
  subject: "Webhook retries timing out after 30s",
  status: "open",
  priority: "high",
  updated: "2h ago",
  created: "Oct 12",
  agent: "Priya Shah",
  unread: 2,
  messages: 6,
  tags: ["API", "integrations"]
}, {
  id: "T-2041",
  subject: "SSO / SAML setup with Okta — metadata URL",
  status: "waiting",
  priority: "medium",
  updated: "Yesterday",
  created: "Oct 10",
  agent: "Daan Visser",
  unread: 0,
  messages: 4,
  tags: ["SSO", "security"]
}, {
  id: "T-2033",
  subject: "Seat count doesn't match Active users in admin",
  status: "open",
  priority: "low",
  updated: "2d ago",
  created: "Oct 09",
  agent: "Priya Shah",
  unread: 0,
  messages: 3,
  tags: ["billing"]
}, {
  id: "T-2012",
  subject: "Bulk import CSV — column mapping question",
  status: "resolved",
  priority: "low",
  updated: "Oct 06",
  created: "Oct 03",
  agent: "Marco Lindt",
  unread: 0,
  messages: 8,
  tags: ["onboarding"]
}, {
  id: "T-1998",
  subject: "Dutch VAT number validation failing on invoices",
  status: "resolved",
  priority: "medium",
  updated: "Sep 29",
  created: "Sep 27",
  agent: "Daan Visser",
  unread: 0,
  messages: 5,
  tags: ["billing", "EU"]
}, {
  id: "T-1974",
  subject: "Custom field 'Renewal owner' not visible in API",
  status: "closed",
  priority: "low",
  updated: "Sep 20",
  created: "Sep 15",
  agent: "Priya Shah",
  unread: 0,
  messages: 4,
  tags: ["API"]
}];

// Contact moments per ticket — each moment has a channel (email/chat/voice/system)
// and a detail payload specific to that channel. Clicking a moment in the timeline
// expands the detail inline.
const CONTACT_MOMENTS = {
  "T-2048": [{
    id: "cm1",
    channel: "email",
    direction: "outbound",
    when: "Oct 12, 09:14",
    summary: "Webhook retries timing out — initial report",
    who: "Elin Karlsson",
    email: {
      from: "elin.karlsson@orbitlabs.io",
      to: "support@incedo.nl",
      subject: "Webhook retries timing out after 30s",
      body: "Hi support,\n\nOur Zapier webhook keeps retrying past 30s and then dropping. We've seen this on about 8% of deal.updated events since Monday.\n\nAnything on your side changed? Happy to share event IDs if it helps.\n\n— Elin\nOrbit Labs · Integrations",
      attachments: [{
        name: "webhook-log-oct11.csv",
        size: "184 KB"
      }]
    }
  }, {
    id: "cm2",
    channel: "email",
    direction: "inbound",
    when: "Oct 12, 10:02",
    summary: "Priya acknowledged, routed to engineering",
    who: "Priya Shah",
    email: {
      from: "priya.shah@incedo.nl",
      to: "elin.karlsson@orbitlabs.io",
      subject: "Re: Webhook retries timing out after 30s",
      body: "Hi Elin,\n\nThanks for the flag. Yes — we rolled out a new outbound queue on Oct 10 that batches webhooks.\n\nThere's a known issue with retry backoff on endpoints that return 202 instead of 200. I'm routing to engineering now and will post an ETA shortly.\n\nIn the meantime, failed events can be re-queued from Admin → API → Failed deliveries.\n\nBest,\nPriya"
    }
  }, {
    id: "cm3",
    channel: "chat",
    direction: "both",
    when: "Oct 12, 14:30",
    summary: "Live chat — confirmed 202 response, discussed ETA",
    who: "Elin ↔ Priya",
    chat: {
      duration: "9 min",
      messages: [{
        who: "you",
        name: "Elin",
        when: "14:30",
        body: "Hey Priya — saw your reply. We do return 202 from Zapier's catch-hook. Is that the trigger?"
      }, {
        who: "agent",
        name: "Priya",
        when: "14:31",
        body: "Almost certainly, yes. Can you paste one of the failing event IDs?"
      }, {
        who: "you",
        name: "Elin",
        when: "14:32",
        body: "evt_9f4a2c1b, evt_9f4a2c4d, evt_9f4a2c88"
      }, {
        who: "agent",
        name: "Priya",
        when: "14:34",
        body: "Got them — traced. Fix is in staging; deploying tomorrow morning CET. I'll update the ticket when it ships."
      }, {
        who: "you",
        name: "Elin",
        when: "14:36",
        body: "Perfect. Thanks for the quick turnaround 🙏"
      }, {
        who: "agent",
        name: "Priya",
        when: "14:37",
        body: "Happy to help. I'll also flag this in the Oct 15 release notes so your team has the context."
      }]
    }
  }, {
    id: "cm4",
    channel: "voice",
    direction: "inbound",
    when: "Oct 12, 16:18",
    summary: "Engineering call-back from Priya — 6m 24s",
    who: "Priya Shah → Elin Karlsson",
    voice: {
      duration: "6:24",
      recording: "priya-callback-oct12.m4a",
      transcript: [{
        who: "agent",
        t: "00:04",
        body: "Hi Elin, it's Priya from Incedo — is this a good time?"
      }, {
        who: "you",
        t: "00:08",
        body: "Yes, go ahead."
      }, {
        who: "agent",
        t: "00:12",
        body: "Quick update — the fix is merged. It'll deploy to prod tomorrow at 07:30 CET. I wanted to walk you through the re-queue workaround in the meantime."
      }, {
        who: "you",
        t: "00:26",
        body: "Sure, that would help. We're sitting on about 140 dropped events."
      }, {
        who: "agent",
        t: "00:34",
        body: "Right — so from Admin, go to API, then Failed deliveries. You'll see a filter for status 202. Select all, hit Re-queue, and the platform will retry them with the old timeout behaviour."
      }, {
        who: "you",
        t: "01:02",
        body: "Got it. And after the prod deploy, will re-queueing still be safe?"
      }, {
        who: "agent",
        t: "01:08",
        body: "Yes — re-queue is idempotent on our side, we dedupe by event ID. Worst case you'll get a 409 for ones already delivered."
      }, {
        who: "you",
        t: "01:24",
        body: "Perfect. I'll action that now. Thanks for the call."
      }, {
        who: "agent",
        t: "01:28",
        body: "Any time. I'll close the loop in the ticket once prod is green."
      }]
    }
  }, {
    id: "cm5",
    channel: "system",
    when: "Oct 13, 08:00",
    summary: "Linked to incident INC-0412 · status page partially degraded"
  }, {
    id: "cm6",
    channel: "email",
    direction: "outbound",
    when: "Oct 13, 09:45",
    summary: "Elin confirmed re-queue succeeded",
    who: "Elin Karlsson",
    email: {
      from: "elin.karlsson@orbitlabs.io",
      to: "support@incedo.nl",
      subject: "Re: Webhook retries timing out after 30s",
      body: "Re-queued ~140 events, all delivered on second attempt. Waiting on the prod fix tomorrow morning.\n\nThanks for the quick turnaround — really appreciated.\n\n— Elin"
    }
  }]
};

// ── Shared bits ─────────────────────────────────────────────────────────
function PillLabel({
  children,
  tone = "muted"
}) {
  const map = {
    muted: ["var(--sp-text-muted)", "var(--sp-surface-2)"],
    info: ["#1A73E8", "rgba(26,115,232,.12)"],
    mint: ["var(--sp-accent-mint)", "rgba(0,184,148,.14)"],
    plum: ["var(--sp-accent-plum)", "rgba(108,92,231,.14)"],
    warm: ["#D93025", "rgba(217,48,37,.12)"],
    amber: ["#B26A00", "rgba(249,168,37,.16)"]
  };
  const [c, bg] = map[tone] || map.muted;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      font: "500 11px/14px Roboto",
      color: c,
      background: bg,
      padding: "3px 8px",
      borderRadius: 999,
      letterSpacing: "0.02em"
    }
  }, children);
}
function TicketStatusChip({
  status,
  priority
}) {
  const sMap = {
    open: {
      tone: "info",
      label: "Open"
    },
    waiting: {
      tone: "amber",
      label: "Waiting on you"
    },
    resolved: {
      tone: "mint",
      label: "Resolved"
    },
    closed: {
      tone: "muted",
      label: "Closed"
    }
  };
  const {
    tone,
    label
  } = sMap[status] || sMap.open;
  return /*#__PURE__*/React.createElement(PillLabel, {
    tone: tone
  }, "\u25CF ", label);
}
function PriorityDot({
  priority
}) {
  const c = priority === "high" ? "#D93025" : priority === "medium" ? "#F9A825" : "var(--sp-text-subtle)";
  return /*#__PURE__*/React.createElement("span", {
    title: `${priority} priority`,
    style: {
      display: "inline-block",
      width: 8,
      height: 8,
      borderRadius: "50%",
      background: c
    }
  });
}

// ── PRODUCTS ────────────────────────────────────────────────────────────
function PProducts({
  state,
  onAdd
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 260,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement("div", {
    className: "sp-grid-2"
  }, [0, 1, 2, 3].map(i => /*#__PURE__*/React.createElement(PCard, {
    key: i
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 120
  })))));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25C6",
    title: "No products yet",
    body: "Your plan doesn't include any active products. Browse the catalog to get started."
  }));
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Products"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 28
    }
  }, "Everything ", ME.customer, " is using, plus add-ons you can turn on."), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 12
    }
  }, "In your plan"), /*#__PURE__*/React.createElement("div", {
    className: "sp-grid-2"
  }, MY_PRODUCTS.map(p => /*#__PURE__*/React.createElement(PCard, {
    key: p.id,
    hover: true,
    style: {
      display: "flex",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 44,
      height: 44,
      borderRadius: 11,
      flexShrink: 0,
      background: `${p.color}22`,
      color: p.color,
      font: "600 20px/44px Roboto",
      textAlign: "center"
    }
  }, p.icon), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, p.name), p.included ? /*#__PURE__*/React.createElement(PillLabel, {
    tone: "mint"
  }, "Included") : /*#__PURE__*/React.createElement(PillLabel, {
    tone: "plum"
  }, "Add-on \xB7 ", fmtMoney(p.price), "/mo")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, p.desc), (p.seats || p.runs || p.reqs) && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 11px/14px 'Roboto Mono',monospace",
      color: "var(--sp-text-muted)",
      marginBottom: 4
    }
  }, /*#__PURE__*/React.createElement("span", null, p.seats ? `${Math.round(p.seats * p.usage / 100)} / ${p.seats} seats` : p.runs ? `${p.runs.toLocaleString()} / ${p.cap.toLocaleString()} runs` : `${p.reqs.toLocaleString()} / ${p.cap.toLocaleString()} req`), /*#__PURE__*/React.createElement("span", null, p.seats ? `${p.usage}%` : `${Math.round((p.runs || p.reqs) / p.cap * 100)}%`)), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 6,
      borderRadius: 3,
      background: "var(--sp-surface-2)",
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      width: `${p.seats ? p.usage : Math.round((p.runs || p.reqs) / p.cap * 100)}%`,
      background: p.color,
      borderRadius: 3
    }
  }))))))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 36,
      marginBottom: 12
    }
  }, "Add-ons available"), /*#__PURE__*/React.createElement("div", {
    className: "sp-grid-2"
  }, PRODUCT_CATALOG.map(p => /*#__PURE__*/React.createElement(PCard, {
    key: p.id,
    hover: true,
    style: {
      position: "relative"
    }
  }, p.tag && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 12,
      right: 12
    }
  }, /*#__PURE__*/React.createElement(PillLabel, {
    tone: p.tag === "New" ? "plum" : "mint"
  }, p.tag)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, p.name), /*#__PURE__*/React.createElement("span", {
    className: "sp-money",
    style: {
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: p.color
    }
  }, "+", fmtMoney(p.price), "/mo")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, p.desc), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => onAdd(p)
  }, "Add to plan"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Learn more"))))));
}

// ── TICKETS LIST ────────────────────────────────────────────────────────
function PTickets({
  state,
  onOpen,
  onNew
}) {
  const [filter, setFilter] = React.useState("all");
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 220,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 6
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u2709",
    title: "No tickets yet",
    body: "You haven't opened any support requests. We usually respond within 2 business hours.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: onNew
    }, "New ticket")
  }));
  const filtered = filter === "all" ? TICKETS : TICKETS.filter(t => filter === "open" ? t.status === "open" || t.status === "waiting" : t.status === filter);
  const counts = {
    all: TICKETS.length,
    open: TICKETS.filter(t => t.status === "open" || t.status === "waiting").length,
    resolved: TICKETS.filter(t => t.status === "resolved").length,
    closed: TICKETS.filter(t => t.status === "closed").length
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-end",
      justifyContent: "space-between",
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Support tickets"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "We typically respond within 2 business hours on business plans.")), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    }),
    onClick: onNew
  }, "New ticket")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 4,
      marginBottom: 16,
      borderBottom: "1px solid var(--sp-border)"
    }
  }, [{
    k: "all",
    label: "All"
  }, {
    k: "open",
    label: "Open"
  }, {
    k: "resolved",
    label: "Resolved"
  }, {
    k: "closed",
    label: "Closed"
  }].map(t => {
    const on = filter === t.k;
    return /*#__PURE__*/React.createElement("span", {
      key: t.k,
      onClick: () => setFilter(t.k),
      style: {
        padding: "10px 14px",
        cursor: "pointer",
        font: `${on ? 600 : 400} 13px/18px Roboto`,
        color: on ? "var(--sp-text)" : "var(--sp-text-muted)",
        borderBottom: on ? "2px solid #1A73E8" : "2px solid transparent",
        marginBottom: -1
      }
    }, t.label, " ", /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-subtle)",
        font: "400 12px/16px 'Roboto Mono',monospace",
        marginLeft: 4
      }
    }, counts[t.k]));
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, filtered.map((t, i) => /*#__PURE__*/React.createElement("div", {
    key: t.id,
    onClick: () => onOpen(t),
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      padding: "16px 20px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      cursor: "pointer"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement(PriorityDot, {
    priority: t.priority
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, t.id), /*#__PURE__*/React.createElement("span", {
    style: {
      font: `${t.unread > 0 ? 600 : 500} 14px/20px Roboto`,
      color: "var(--sp-text)",
      textOverflow: "ellipsis",
      overflow: "hidden",
      whiteSpace: "nowrap"
    }
  }, t.subject), t.unread > 0 && /*#__PURE__*/React.createElement("span", {
    style: {
      background: "#1A73E8",
      color: "#fff",
      font: "600 10px/14px Roboto",
      padding: "2px 6px",
      borderRadius: 999
    }
  }, t.unread, " new")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      marginTop: 6,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(TicketStatusChip, {
    status: t.status
  }), t.tags.map(tag => /*#__PURE__*/React.createElement(PillLabel, {
    key: tag,
    tone: "muted"
  }, tag)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "\xB7 ", t.messages, " messages \xB7 with ", t.agent))), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)",
      whiteSpace: "nowrap"
    }
  }, t.updated)))), filtered.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "48px 24px",
      textAlign: "center",
      color: "var(--sp-text-muted)"
    }
  }, "No tickets in this view."));
}

// ── TICKET DETAIL (ticket info + contact moments timeline) ─────────────
// Structure:
// 1. Ticket details card (subject, status, priority, who handles it, tags)
// 2. "Contact moments" list — one row per touchpoint (email / chat / voice / system).
//    Each row is expandable: clicking opens the channel-specific content below.
// 3. Reply composer (when ticket is active)

function ChannelIcon({
  channel,
  size = 36
}) {
  const map = {
    email: {
      g: "✉",
      bg: "rgba(26,115,232,.12)",
      c: "#1A73E8"
    },
    chat: {
      g: "◆",
      bg: "rgba(108,92,231,.14)",
      c: "var(--sp-accent-plum)"
    },
    voice: {
      g: "☎",
      bg: "rgba(0,184,148,.14)",
      c: "var(--sp-accent-mint)"
    },
    whatsapp: {
      g: "◉",
      bg: "rgba(37,211,102,.14)",
      c: "#25D366"
    },
    sms: {
      g: "#",
      bg: "rgba(249,168,37,.16)",
      c: "#F9A825"
    },
    portal: {
      g: "◈",
      bg: "rgba(108,92,231,.12)",
      c: "var(--sp-accent-plum)"
    },
    system: {
      g: "◌",
      bg: "var(--sp-surface-2)",
      c: "var(--sp-text-muted)"
    }
  };
  const {
    g,
    bg,
    c
  } = map[channel] || map.system;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: size >= 32 ? 10 : 6,
      background: bg,
      color: c,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      font: `600 ${Math.round(size * 0.48)}px/1 Roboto`,
      flexShrink: 0
    }
  }, g);
}
function ChannelLabel({
  channel,
  direction
}) {
  const label = channel === "email" ? "Email" : channel === "chat" ? "Live chat" : channel === "voice" ? "Voice call" : channel === "whatsapp" ? "WhatsApp" : channel === "sms" ? "SMS" : channel === "portal" ? "Customer portal" : "System event";
  const dir = direction === "outbound" ? "↑ from you" : direction === "inbound" ? "↓ from support" : direction === "both" ? "↔ two-way" : null;
  return /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 12px/16px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "0.02em"
    }
  }, label), dir && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "\xB7 ", dir));
}

// Portal message — plain body + note telling support how the customer wants to be reached back
function PortalDetail({
  data
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 20px",
      background: "var(--sp-surface-2)",
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16,
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/22px Roboto",
      color: "var(--sp-text)",
      whiteSpace: "pre-wrap",
      marginBottom: 12
    }
  }, data.body), data.replyBackVia && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "10px 14px",
      background: "rgba(37,211,102,.08)",
      border: "1px solid rgba(37,211,102,.25)",
      borderRadius: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: "#25D366",
      flexShrink: 0
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Support will reply via ", /*#__PURE__*/React.createElement("b", null, data.replyBackVia), data.replyBackHint && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, " \xB7 ", data.replyBackHint))));
}

// Detail bodies — one per channel
function EmailDetail({
  data
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 20px",
      background: "var(--sp-surface-2)",
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "64px 1fr",
      rowGap: 4,
      columnGap: 12,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("span", null, "From"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)"
    }
  }, data.from), /*#__PURE__*/React.createElement("span", null, "To"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)"
    }
  }, data.to), /*#__PURE__*/React.createElement("span", null, "Subject"), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text)",
      fontWeight: 500
    }
  }, data.subject)), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16,
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/22px Roboto",
      color: "var(--sp-text)",
      whiteSpace: "pre-wrap"
    }
  }, data.body), data.attachments && data.attachments.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      gap: 8,
      flexWrap: "wrap"
    }
  }, data.attachments.map((a, i) => /*#__PURE__*/React.createElement("span", {
    key: i,
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      padding: "8px 12px",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)"
    }
  }, "\u2398"), a.name, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)",
      font: "400 11px/14px Roboto"
    }
  }, a.size)))));
}
function ChatDetail({
  data,
  variant = "chat"
}) {
  // variant: "chat" (plum/blue) or "whatsapp" (green) or "sms" (amber)
  const mineBg = variant === "whatsapp" ? "#DCF8C6" : variant === "sms" ? "#FEF3C7" : "#1A73E8";
  const mineColor = variant === "whatsapp" || variant === "sms" ? "var(--sp-text)" : "#fff";
  const mineBorder = variant === "whatsapp" ? "1px solid #BEEBA3" : variant === "sms" ? "1px solid #FCD34D" : "none";
  const header = variant === "whatsapp" ? "WhatsApp conversation" : variant === "sms" ? "SMS conversation" : "Live chat";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 20px",
      background: "var(--sp-surface-2)",
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 12
    }
  }, header, " \xB7 duration ", data.duration || data.messages.length + " messages", " \xB7 ", data.messages.length, " messages"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, data.messages.map((m, i) => {
    const mine = m.who === "you";
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        display: "flex",
        justifyContent: mine ? "flex-end" : "flex-start"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        maxWidth: "76%"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-subtle)",
        marginBottom: 3,
        textAlign: mine ? "right" : "left"
      }
    }, mine ? "You" : m.name, " \xB7 ", m.when), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: "10px 14px",
        background: mine ? mineBg : "var(--sp-surface)",
        color: mine ? mineColor : "var(--sp-text)",
        border: mine ? mineBorder : "1px solid var(--sp-border)",
        borderRadius: 14,
        borderBottomRightRadius: mine ? 4 : 14,
        borderBottomLeftRadius: mine ? 14 : 4,
        font: "400 13px/20px Roboto"
      }
    }, m.body)));
  })));
}
function VoiceDetail({
  data
}) {
  // Fake audio scrubber — aesthetic only, no real playback
  const [playing, setPlaying] = React.useState(false);
  const [pos, setPos] = React.useState(0);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 20px",
      background: "var(--sp-surface-2)",
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      padding: "14px 16px",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 10,
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: () => setPlaying(p => !p),
    style: {
      width: 38,
      height: 38,
      borderRadius: "50%",
      background: "var(--sp-accent-mint)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      color: "#fff",
      font: "700 14px/1 Roboto",
      cursor: "pointer",
      flexShrink: 0
    }
  }, playing ? "❚❚" : "▶"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, data.recording), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      height: 4,
      background: "var(--sp-border)",
      borderRadius: 999,
      position: "relative",
      cursor: "pointer"
    },
    onClick: e => {
      const r = e.currentTarget.getBoundingClientRect();
      setPos(Math.max(0, Math.min(1, (e.clientX - r.left) / r.width)));
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: `${pos * 100}%`,
      background: "var(--sp-accent-mint)",
      borderRadius: 999
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: `calc(${pos * 100}% - 6px)`,
      top: -4,
      width: 12,
      height: 12,
      borderRadius: "50%",
      background: "#fff",
      border: "2px solid var(--sp-accent-mint)"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      marginTop: 6,
      font: "400 11px/14px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, /*#__PURE__*/React.createElement("span", null, Math.floor(pos * parseInt(data.duration.split(":")[0] * 60) + pos * parseInt(data.duration.split(":")[1] || 0)), "s"), /*#__PURE__*/React.createElement("span", null, data.duration))), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      cursor: "pointer"
    }
  }, "Download")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 12px/16px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10,
      letterSpacing: "0.02em",
      textTransform: "uppercase"
    }
  }, "Transcript"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, data.transcript.map((line, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      flexShrink: 0,
      width: 42,
      font: "500 12px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, line.t), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: line.who === "you" ? "#1A73E8" : "var(--sp-accent-mint)",
      marginBottom: 2
    }
  }, line.who === "you" ? "You" : "Support"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, line.body))))));
}
function PTicket({
  ticket,
  onBack
}) {
  const [reply, setReply] = React.useState("");
  const [openId, setOpenId] = React.useState(null);
  // Moments are local state so "Send reply" can append a new moment to the timeline.
  const [moments, setMoments] = React.useState(() => CONTACT_MOMENTS[ticket.id] || []);
  // Ticket status is local state too so "Mark as resolved" can actually close the ticket.
  const [status, setStatus] = React.useState(ticket.status);
  // Customer's preferred channel for support to REPLY BACK through.
  // Defaults to WhatsApp. Can be changed per-ticket.
  const [replyBackChannel, setReplyBackChannel] = React.useState(ticket.preferredChannel || "whatsapp");
  const [channelMenuOpen, setChannelMenuOpen] = React.useState(false);
  const ticketWithStatus = {
    ...ticket,
    status
  };
  const CHANNELS = [{
    k: "whatsapp",
    label: "WhatsApp",
    hint: "+31 6 1234 5678"
  }, {
    k: "email",
    label: "Email",
    hint: "elin@orbitlabs.io"
  }, {
    k: "chat",
    label: "Live chat",
    hint: "web widget"
  }, {
    k: "sms",
    label: "SMS",
    hint: "+31 6 1234 5678"
  }, {
    k: "voice",
    label: "Voice call",
    hint: "call-back during business hours"
  }];
  const currentReplyBack = CHANNELS.find(c => c.k === replyBackChannel) || CHANNELS[0];
  const sendReply = () => {
    const text = reply.trim();
    if (!text) return;
    const now = new Date();
    const mo = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][now.getMonth()];
    const when = `${mo} ${String(now.getDate()).padStart(2, "0")}, ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    const timeShort = `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;

    // Outbound moment from the portal — channel is always "portal".
    // The reply-back preference determines how support will respond, which gets noted on the moment.
    const newMoment = {
      id: "cm-" + Date.now(),
      channel: "portal",
      direction: "outbound",
      when,
      summary: text.split("\n")[0].slice(0, 80) + (text.length > 80 ? "…" : ""),
      who: "Elin Karlsson · via Customer Portal",
      portal: {
        body: text,
        replyBackVia: currentReplyBack.label,
        replyBackHint: currentReplyBack.hint
      }
    };
    setMoments(prev => [...prev, newMoment]);
    setOpenId(newMoment.id); // auto-expand the new one
    setReply("");
    if (status === "waiting") setStatus("open");
  };
  const markResolved = () => {
    setStatus("resolved");
    const now = new Date();
    const mo = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][now.getMonth()];
    const when = `${mo} ${String(now.getDate()).padStart(2, "0")}, ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    setMoments(prev => [...prev, {
      id: "cm-sys-" + Date.now(),
      channel: "system",
      when,
      summary: "Ticket marked as resolved by Elin Karlsson"
    }]);
  };
  const reopen = () => {
    setStatus("open");
    const now = new Date();
    const mo = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"][now.getMonth()];
    const when = `${mo} ${String(now.getDate()).padStart(2, "0")}, ${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`;
    setMoments(prev => [...prev, {
      id: "cm-sys-" + Date.now(),
      channel: "system",
      when,
      summary: "Ticket reopened by Elin Karlsson"
    }]);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 16,
      display: "inline-block"
    }
  }, "\u2190 Back to tickets"), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden",
      marginBottom: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "22px 26px"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, ticket.id), /*#__PURE__*/React.createElement(TicketStatusChip, {
    status: status
  }), /*#__PURE__*/React.createElement(PillLabel, {
    tone: ticket.priority === "high" ? "warm" : ticket.priority === "medium" ? "amber" : "muted"
  }, /*#__PURE__*/React.createElement(PriorityDot, {
    priority: ticket.priority
  }), " ", ticket.priority, " priority")), /*#__PURE__*/React.createElement("h2", {
    style: {
      font: "700 22px/28px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.01em"
    }
  }, ticket.subject), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))",
      gap: 16,
      marginTop: 20,
      paddingTop: 20,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      letterSpacing: "0.06em",
      textTransform: "uppercase",
      marginBottom: 4
    }
  }, "Opened"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, ticket.created)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      letterSpacing: "0.06em",
      textTransform: "uppercase",
      marginBottom: 4
    }
  }, "Last update"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, ticket.updated)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      letterSpacing: "0.06em",
      textTransform: "uppercase",
      marginBottom: 4
    }
  }, "Assigned to"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: ticket.agent,
    size: 22
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, ticket.agent))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-subtle)",
      letterSpacing: "0.06em",
      textTransform: "uppercase",
      marginBottom: 4
    }
  }, "Contact moments"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, moments.length))), ticket.tags.length > 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      marginTop: 16,
      flexWrap: "wrap"
    }
  }, ticket.tags.map(t => /*#__PURE__*/React.createElement(PillLabel, {
    key: t,
    tone: "muted"
  }, "#", t))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 12,
      display: "flex",
      alignItems: "baseline",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("h3", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)",
      margin: 0
    }
  }, "Customer contact moments"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Tap a row to view the full content")), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, moments.map((m, i) => {
    const isOpen = openId === m.id;
    const isSystem = m.channel === "system";
    const last = i === moments.length - 1;
    return /*#__PURE__*/React.createElement("div", {
      key: m.id,
      style: {
        borderBottom: last && !isOpen ? "none" : "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      onClick: () => !isSystem && setOpenId(isOpen ? null : m.id),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 14,
        padding: "14px 20px",
        cursor: isSystem ? "default" : "pointer",
        background: isOpen ? "var(--sp-surface-2)" : "transparent",
        transition: "background 120ms"
      },
      onMouseEnter: e => {
        if (!isSystem && !isOpen) e.currentTarget.style.background = "var(--sp-surface-2)";
      },
      onMouseLeave: e => {
        if (!isSystem && !isOpen) e.currentTarget.style.background = "transparent";
      }
    }, /*#__PURE__*/React.createElement(ChannelIcon, {
      channel: m.channel
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 10,
        marginBottom: 4,
        flexWrap: "wrap"
      }
    }, /*#__PURE__*/React.createElement(ChannelLabel, {
      channel: m.channel,
      direction: m.direction
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, "\xB7 ", m.when)), /*#__PURE__*/React.createElement("div", {
      style: {
        font: `${isSystem ? 400 : 500} 14px/20px Roboto`,
        color: isSystem ? "var(--sp-text-muted)" : "var(--sp-text)",
        textOverflow: "ellipsis",
        overflow: "hidden",
        whiteSpace: "nowrap"
      }
    }, m.summary), m.who && !isSystem && /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, m.who)), !isSystem && /*#__PURE__*/React.createElement("span", {
      style: {
        flexShrink: 0,
        width: 24,
        height: 24,
        borderRadius: "50%",
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        color: "var(--sp-text-muted)",
        font: "500 12px/1 Roboto",
        transform: isOpen ? "rotate(180deg)" : "rotate(0)",
        transition: "transform 160ms"
      }
    }, "\u25BE")), isOpen && m.channel === "email" && /*#__PURE__*/React.createElement(EmailDetail, {
      data: m.email
    }), isOpen && m.channel === "chat" && /*#__PURE__*/React.createElement(ChatDetail, {
      data: m.chat,
      variant: "chat"
    }), isOpen && m.channel === "whatsapp" && /*#__PURE__*/React.createElement(ChatDetail, {
      data: m.chat,
      variant: "whatsapp"
    }), isOpen && m.channel === "sms" && /*#__PURE__*/React.createElement(ChatDetail, {
      data: m.chat,
      variant: "sms"
    }), isOpen && m.channel === "voice" && /*#__PURE__*/React.createElement(VoiceDetail, {
      data: m.voice
    }), isOpen && m.channel === "portal" && /*#__PURE__*/React.createElement(PortalDetail, {
      data: m.portal
    }));
  })), (status === "open" || status === "waiting") && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 12,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Reply"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "\xB7 sent from the customer portal")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 14,
      padding: "10px 14px",
      background: "var(--sp-surface-2)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Support should reply via"), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: () => setChannelMenuOpen(o => !o),
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 8,
      cursor: "pointer",
      padding: "6px 12px",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 6,
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement(ChannelIcon, {
    channel: replyBackChannel,
    size: 20
  }), currentReplyBack.label, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)",
      fontSize: 10
    }
  }, "\u25BE")), channelMenuOpen && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: "calc(100% + 4px)",
      left: 0,
      zIndex: 10,
      minWidth: 240,
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      boxShadow: "var(--sp-shadow-md)",
      overflow: "hidden"
    }
  }, CHANNELS.map(c => /*#__PURE__*/React.createElement("div", {
    key: c.k,
    onClick: () => {
      setReplyBackChannel(c.k);
      setChannelMenuOpen(false);
    },
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "10px 14px",
      cursor: "pointer",
      background: c.k === replyBackChannel ? "var(--sp-surface-2)" : "transparent",
      borderBottom: "1px solid var(--sp-border)"
    },
    onMouseEnter: e => {
      if (c.k !== replyBackChannel) e.currentTarget.style.background = "var(--sp-surface-2)";
    },
    onMouseLeave: e => {
      if (c.k !== replyBackChannel) e.currentTarget.style.background = "transparent";
    }
  }, /*#__PURE__*/React.createElement(ChannelIcon, {
    channel: c.k,
    size: 22
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, c.label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, c.hint)), c.k === replyBackChannel && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "#1A73E8",
      font: "500 12px/1 Roboto"
    }
  }, "\u2713"))))), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "\xB7 ", currentReplyBack.hint)), /*#__PURE__*/React.createElement("textarea", {
    value: reply,
    onChange: e => setReply(e.target.value),
    placeholder: "Type your message\u2026",
    style: {
      width: "100%",
      minHeight: 96,
      resize: "vertical",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      padding: 12,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      marginTop: 10,
      flexWrap: "wrap",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2398"
    })
  }, "Attach file")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: markResolved
  }, "Mark as resolved"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    disabled: !reply.trim(),
    onClick: sendReply
  }, "Send")))), (status === "resolved" || status === "closed") && /*#__PURE__*/React.createElement(PCard, {
    style: {
      marginTop: 18,
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "This ticket is ", status, ".", " ", /*#__PURE__*/React.createElement("span", {
    onClick: reopen,
    style: {
      color: "#1A73E8",
      cursor: "pointer",
      fontWeight: 500
    }
  }, "Reopen"), " ", "or", " ", /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      color: "#1A73E8",
      cursor: "pointer",
      fontWeight: 500
    }
  }, "back to list"), ".")));
}

// ── NEW TICKET ─────────────────────────────────────────────────────────
// A focused composer: category → subject → description → priority → channel preference → submit.
function PNewTicket({
  onCancel,
  onSubmit
}) {
  const [category, setCategory] = React.useState(null);
  const [subject, setSubject] = React.useState("");
  const [body, setBody] = React.useState("");
  const [priority, setPriority] = React.useState("medium");
  const [channel, setChannel] = React.useState("email");
  const [files, setFiles] = React.useState([]);
  const categories = [{
    id: "billing",
    label: "Billing & invoices",
    desc: "Subscription, VAT, payment methods",
    icon: "€",
    color: "#F9A825"
  }, {
    id: "technical",
    label: "Technical issue",
    desc: "Something broken, error, data not syncing",
    icon: "⚠",
    color: "#D93025"
  }, {
    id: "integration",
    label: "API & integrations",
    desc: "Webhooks, Zapier, custom integrations",
    icon: "⧉",
    color: "var(--sp-accent-plum)"
  }, {
    id: "howto",
    label: "How do I…",
    desc: "Usage questions, feature walkthroughs",
    icon: "?",
    color: "#1A73E8"
  }, {
    id: "account",
    label: "Account & access",
    desc: "SSO, seats, users, permissions",
    icon: "◐",
    color: "var(--sp-accent-mint)"
  }, {
    id: "other",
    label: "Something else",
    desc: "We'll triage it for you",
    icon: "◇",
    color: "#546E7A"
  }];
  const canSubmit = category && subject.trim().length >= 3 && body.trim().length >= 3;
  const addFile = () => {
    // Mock file picker — generates a fake file
    const mockNames = ["screenshot.png", "console-log.txt", "api-trace.har", "export.csv"];
    const mockSizes = ["48 KB", "12 KB", "2.1 MB", "184 KB"];
    const i = files.length % mockNames.length;
    setFiles(f => [...f, {
      name: mockNames[i],
      size: mockSizes[i]
    }]);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onCancel,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 16,
      display: "inline-block"
    }
  }, "\u2190 Back to tickets"), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "New support ticket"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 28
    }
  }, "Business-hour response within 2 hours \xB7 urgent issues within 15 minutes on Scale plans."), /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 28
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "1. What's this about?"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
      gap: 10
    }
  }, categories.map(cat => {
    const on = category === cat.id;
    return /*#__PURE__*/React.createElement("div", {
      key: cat.id,
      onClick: () => setCategory(cat.id),
      style: {
        display: "flex",
        gap: 12,
        alignItems: "flex-start",
        padding: on ? "13px 13px" : "14px 14px",
        background: on ? "color-mix(in oklab, " + (cat.color.startsWith("var") ? "#2ECC9B" : cat.color) + " 6%, var(--sp-surface))" : "var(--sp-surface)",
        border: on ? `2px solid ${cat.color}` : "1px solid var(--sp-border)",
        borderRadius: 10,
        cursor: "pointer",
        transition: "border-color 120ms, background 120ms"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 32,
        height: 32,
        borderRadius: 8,
        flexShrink: 0,
        background: `${cat.color.startsWith("var") ? "rgba(0,184,148,.14)" : cat.color + "1f"}`,
        color: cat.color,
        display: "inline-flex",
        alignItems: "center",
        justifyContent: "center",
        font: "600 16px/1 Roboto"
      }
    }, cat.icon), /*#__PURE__*/React.createElement("div", {
      style: {
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "600 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, cat.label), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, cat.desc)));
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 28
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "2. Describe the problem"), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 4
    }
  }, "Subject"), /*#__PURE__*/React.createElement("input", {
    value: subject,
    onChange: e => setSubject(e.target.value),
    placeholder: "One-line summary (e.g. Webhook retries are failing on 202)",
    style: {
      width: "100%",
      height: 40,
      padding: "0 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 4
    }
  }, "Description"), /*#__PURE__*/React.createElement("textarea", {
    value: body,
    onChange: e => setBody(e.target.value),
    placeholder: "What happened, what you expected, and any steps to reproduce. Paste event IDs or error messages if you have them.",
    style: {
      width: "100%",
      minHeight: 140,
      resize: "vertical",
      padding: 12,
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      font: "400 14px/22px Roboto",
      color: "var(--sp-text)",
      background: "var(--sp-surface)",
      outline: "none"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      gap: 8,
      alignItems: "center",
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2398"
    }),
    onClick: addFile
  }, "Attach file"), files.map((f, i) => /*#__PURE__*/React.createElement("span", {
    key: i,
    style: {
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      padding: "4px 8px",
      background: "var(--sp-surface-2)",
      borderRadius: 6,
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, f.name, " ", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)"
    }
  }, f.size), /*#__PURE__*/React.createElement("span", {
    onClick: () => setFiles(files.filter((_, j) => j !== i)),
    style: {
      cursor: "pointer",
      color: "var(--sp-text-muted)",
      marginLeft: 2
    }
  }, "\u2715")))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 28
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 10
    }
  }, "3. Priority & how we should reach you"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(auto-fit, minmax(260px, 1fr))",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 10
    }
  }, "Priority"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, [{
    k: "low",
    label: "Low",
    desc: "No rush — a few days is fine."
  }, {
    k: "medium",
    label: "Medium",
    desc: "Affecting us but we have a workaround."
  }, {
    k: "high",
    label: "High",
    desc: "Blocking production work right now."
  }].map(p => {
    const on = priority === p.k;
    return /*#__PURE__*/React.createElement("label", {
      key: p.k,
      style: {
        display: "flex",
        gap: 10,
        alignItems: "flex-start",
        padding: "10px 12px",
        borderRadius: 8,
        cursor: "pointer",
        background: on ? "var(--sp-surface-2)" : "transparent",
        border: on ? "1px solid var(--sp-border)" : "1px solid transparent"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 16,
        height: 16,
        borderRadius: "50%",
        flexShrink: 0,
        marginTop: 2,
        border: `2px solid ${on ? "#1A73E8" : "var(--sp-border)"}`,
        background: on ? "radial-gradient(#1A73E8 40%, #fff 45%)" : "var(--sp-surface)"
      }
    }), /*#__PURE__*/React.createElement("div", {
      onClick: () => setPriority(p.k),
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement(PriorityDot, {
      priority: p.k
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, p.label)), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, p.desc)));
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 10
    }
  }, "Preferred channel"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, [{
    k: "email",
    label: "Email",
    desc: "Replies to elin.karlsson@orbitlabs.io.",
    icon: "✉"
  }, {
    k: "chat",
    label: "Live chat",
    desc: "We'll ping you when an agent is free.",
    icon: "◆"
  }, {
    k: "voice",
    label: "Phone call",
    desc: "Available 09:00–18:00 CET weekdays.",
    icon: "☎"
  }].map(ch => {
    const on = channel === ch.k;
    return /*#__PURE__*/React.createElement("div", {
      key: ch.k,
      onClick: () => setChannel(ch.k),
      style: {
        display: "flex",
        gap: 10,
        alignItems: "center",
        padding: "10px 12px",
        borderRadius: 8,
        cursor: "pointer",
        background: on ? "var(--sp-surface-2)" : "transparent",
        border: on ? "1px solid var(--sp-border)" : "1px solid transparent"
      }
    }, /*#__PURE__*/React.createElement(ChannelIcon, {
      channel: ch.k,
      size: 28
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, ch.label), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, ch.desc)), /*#__PURE__*/React.createElement("span", {
      style: {
        width: 16,
        height: 16,
        borderRadius: "50%",
        flexShrink: 0,
        border: `2px solid ${on ? "#1A73E8" : "var(--sp-border)"}`,
        background: on ? "radial-gradient(#1A73E8 40%, #fff 45%)" : "transparent"
      }
    }));
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      justifyContent: "space-between",
      gap: 14,
      padding: "16px 20px",
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 10,
      flexWrap: "wrap"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, canSubmit ? "Ready to submit — we'll email you the ticket ID immediately." : "Pick a category and fill in the subject + description to continue."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    onClick: onCancel
  }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    disabled: !canSubmit,
    onClick: () => onSubmit({
      category,
      subject,
      body,
      priority,
      channel,
      files
    })
  }, "Submit ticket"))));
}
Object.assign(window, {
  MY_PRODUCTS,
  PRODUCT_CATALOG,
  TICKETS,
  CONTACT_MOMENTS,
  PillLabel,
  TicketStatusChip,
  PriorityDot,
  ChannelIcon,
  ChannelLabel,
  EmailDetail,
  ChatDetail,
  VoiceDetail,
  PProducts,
  PTickets,
  PTicket,
  PNewTicket
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/portal_Extra.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/portal_Screens.jsx
try { (() => {
// Self-service portal — shell + screens
// Uses the same tokens/data as the admin prototype, but with a customer-first information architecture:
// - Topbar instead of sidebar (wider canvas, less dense)
// - Read-mostly; only editing own payment methods + upgrades
// - Single "my subscription" instead of a list
// - Warmer tone & larger type scale

const {
  useState: useP,
  useEffect: useEP
} = React;

// SideLabel — small uppercase eyebrow used inside portal cards
function SideLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, children);
}
const PORTAL_NAV = [{
  route: "overview",
  label: "Overview"
}, {
  route: "subscription",
  label: "My plan"
}, {
  route: "products",
  label: "Products"
}, {
  route: "billing",
  label: "Billing"
}, {
  route: "payment",
  label: "Payment"
}, {
  route: "tickets",
  label: "Support"
}, {
  route: "complaints",
  label: "Complaints"
}, {
  route: "upgrade",
  label: "Upgrade"
}, {
  route: "account",
  label: "Account"
}];

// Customer identity — this portal represents ONE customer
const ME = SUBS[1]; // Orbit Labs · Growth · 42 seats · active
const MY_INVOICES = INVOICES.filter(i => i.customer === ME.customer).slice(0, 5);
// Also fake a recent "paid" history for portal look
const PORTAL_INVOICES = [{
  id: "INV-20251002",
  amount: 21600,
  status: "paid",
  issued: "Oct 01",
  due: "Oct 15",
  paidOn: "Oct 03"
}, {
  id: "INV-20250902",
  amount: 21600,
  status: "paid",
  issued: "Sep 01",
  due: "Sep 15",
  paidOn: "Sep 03"
}, {
  id: "INV-20250802",
  amount: 19800,
  status: "paid",
  issued: "Aug 01",
  due: "Aug 15",
  paidOn: "Aug 03"
}, {
  id: "INV-20250702",
  amount: 19800,
  status: "paid",
  issued: "Jul 01",
  due: "Jul 15",
  paidOn: "Jul 04"
}, {
  id: "INV-20250602",
  amount: 18000,
  status: "paid",
  issued: "Jun 01",
  due: "Jun 15",
  paidOn: "Jun 03"
}];

// ── Portal shell (topbar instead of sidebar) ────────────────────────────
// Icons for bottom tab bar — simple glyphs
const PORTAL_TAB_ICONS = {
  overview: "⌂",
  subscription: "◈",
  products: "▤",
  billing: "₿",
  upgrade: "▲",
  tickets: "✉",
  account: "◉"
};
function PortalTopBar({
  route,
  onNavigate,
  theme,
  onTheme
}) {
  return /*#__PURE__*/React.createElement("header", {
    className: "sp-topbar-compact",
    style: {
      display: "flex",
      alignItems: "center",
      gap: 32,
      padding: "0 32px",
      height: 64,
      background: "var(--sp-surface)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 30,
      height: 30,
      borderRadius: 9,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff",
      fontWeight: 700,
      fontSize: 13,
      letterSpacing: "-0.04em"
    }
  }, "iC"), /*#__PURE__*/React.createElement("div", {
    className: "sp-topbar-logo-full",
    style: {
      font: "700 16px/20px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.02em"
    }
  }, "Incedo", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontWeight: 400
    }
  }, " / Customer Portal"))), /*#__PURE__*/React.createElement("nav", {
    className: "sp-topbar-nav",
    style: {
      display: "flex",
      gap: 4
    }
  }, PORTAL_NAV.map(n => {
    const on = n.route === route;
    return /*#__PURE__*/React.createElement("span", {
      key: n.route,
      onClick: () => onNavigate(n.route),
      style: {
        padding: "8px 14px",
        borderRadius: 8,
        cursor: "pointer",
        font: `${on ? 600 : 400} 14px/20px Roboto`,
        color: on ? "var(--sp-text)" : "var(--sp-text-muted)",
        background: on ? "var(--sp-surface-2)" : "transparent"
      }
    }, n.label);
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    onClick: onTheme,
    style: {
      cursor: "pointer",
      padding: 8,
      color: "var(--sp-text-muted)",
      fontSize: 15
    }
  }, theme === "dark" ? "☼" : "☾"), /*#__PURE__*/React.createElement("div", {
    className: "sp-hide-mobile",
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "6px 12px 6px 6px",
      borderRadius: 999,
      background: "var(--sp-surface-2)"
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: "Elin Karlsson",
    size: 28
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, "Elin Karlsson"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 10px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, ME.customer, " \xB7 Admin"))), /*#__PURE__*/React.createElement("div", {
    className: "sp-show-mobile-flex",
    style: {
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: "Elin Karlsson",
    size: 32
  })));
}

// Bottom tab bar — visible only below 720px. Shows 4 primary routes + "More" sheet for the rest.
function PortalBottomTabs({
  route,
  onNavigate
}) {
  const [sheetOpen, setSheet] = React.useState(false);
  // 4 primary tabs fit well on smallest phones. Rest go into More sheet.
  const primary = ["overview", "subscription", "billing", "tickets"];
  const primaryNav = primary.map(r => PORTAL_NAV.find(n => n.route === r)).filter(Boolean);
  const moreNav = PORTAL_NAV.filter(n => !primary.includes(n.route));
  const moreActive = moreNav.some(n => n.route === route);
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "sp-bottom-tabs"
  }, primaryNav.map(n => /*#__PURE__*/React.createElement("div", {
    key: n.route,
    className: "sp-tab-btn" + (n.route === route ? " on" : ""),
    onClick: () => onNavigate(n.route)
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-tab-ico"
  }, PORTAL_TAB_ICONS[n.route] || "•"), /*#__PURE__*/React.createElement("div", {
    className: "sp-tab-label"
  }, n.label))), /*#__PURE__*/React.createElement("div", {
    className: "sp-tab-btn" + (moreActive ? " on" : ""),
    onClick: () => setSheet(true)
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-tab-ico"
  }, "\u2261"), /*#__PURE__*/React.createElement("div", {
    className: "sp-tab-label"
  }, "More"))), /*#__PURE__*/React.createElement("div", {
    className: "sp-sheet-backdrop" + (sheetOpen ? " on" : ""),
    onClick: () => setSheet(false)
  }), /*#__PURE__*/React.createElement("div", {
    className: "sp-sheet" + (sheetOpen ? " on" : ""),
    role: "dialog",
    "aria-modal": "true"
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-sheet-handle"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "4px 20px 12px",
      font: "600 13px/16px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "More"), /*#__PURE__*/React.createElement("div", null, moreNav.map(n => /*#__PURE__*/React.createElement("div", {
    key: n.route,
    onClick: () => {
      setSheet(false);
      onNavigate(n.route);
    },
    style: {
      display: "flex",
      alignItems: "center",
      gap: 14,
      padding: "14px 20px",
      cursor: "pointer",
      font: "500 15px/20px Roboto",
      color: n.route === route ? "#1A73E8" : "var(--sp-text)",
      background: n.route === route ? "rgba(26,115,232,0.06)" : "transparent"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 20,
      width: 24,
      display: "inline-flex",
      justifyContent: "center",
      color: n.route === route ? "#1A73E8" : "var(--sp-text-muted)"
    }
  }, PORTAL_TAB_ICONS[n.route] || "•"), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1
    }
  }, n.label), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)"
    }
  }, "\u203A"))))));
}

// ── OVERVIEW ────────────────────────────────────────────────────────────
function POverview({
  state,
  onNavigate
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "40%",
    h: 36
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "60%",
    h: 18
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 32
    }
  }), /*#__PURE__*/React.createElement("div", {
    className: "sp-grid-21",
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 200
  })), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 200
  }))));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u27F3",
    title: "No active subscription",
    body: "You don't have an active plan yet. Contact your account owner or choose a plan to get started.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: () => onNavigate("upgrade")
    }, "Choose a plan")
  }));
  const hasIssue = state === "error" ? false : false; // a good state by default

  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 32px/38px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Welcome back, Elin"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 15px/22px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6,
      marginBottom: 32
    }
  }, "Here's how ", ME.customer, "'s account is doing today."), /*#__PURE__*/React.createElement("div", {
    className: "sp-grid-21",
    style: {
      display: "grid",
      gridTemplateColumns: "2fr 1fr",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "28px 28px 20px",
      background: "linear-gradient(135deg, rgba(26,115,232,.06), rgba(108,92,231,.06))"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.08em"
    }
  }, "Your plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 10,
      marginTop: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-display",
    style: {
      fontSize: 34,
      color: "var(--sp-text)"
    }
  }, ME.plan), /*#__PURE__*/React.createElement(StatusChip, {
    s: ME.status
  })), /*#__PURE__*/React.createElement("div", {
    className: "sp-money",
    style: {
      font: "500 15px/22px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, fmtMoney(ME.mrr), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontSize: 13
    }
  }, " / month \xB7 ", ME.seats, " seats"))), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: () => onNavigate("upgrade"),
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2191"
    })
  }, "Upgrade plan"))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 28px",
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: 18,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Seats used"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 20px/26px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, Math.round(ME.seats * ME.usage / 100), " / ", ME.seats), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6,
      height: 6,
      borderRadius: 3,
      background: "var(--sp-surface-2)",
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      width: `${ME.usage}%`,
      background: "var(--sp-accent-mint)",
      borderRadius: 3
    }
  }))), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Next charge"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 20px/26px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, ME.next), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Visa \u2022\u2022 4242")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Customer since"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 20px/26px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, ME.since.slice(0, 4)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, Math.round((2025 - parseInt(ME.since.slice(0, 4))) * 12 + 10), " months")))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Next invoice"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 28,
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, fmtMoney(ME.mrr)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Scheduled for ", ME.next, ", 2025"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: () => onNavigate("billing")
  }, "View billing"))), /*#__PURE__*/React.createElement(PCard, {
    style: {
      background: "linear-gradient(135deg, rgba(0,184,148,.08), transparent)"
    }
  }, /*#__PURE__*/React.createElement(SideLabel, null, "Account health"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 10,
      marginTop: 6
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-display",
    style: {
      fontSize: 28,
      color: "var(--sp-accent-mint)"
    }
  }, "Excellent")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, "All invoices paid on time. No usage anomalies detected.")))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 28
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)",
      marginBottom: 12
    }
  }, "Recent activity"), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, [{
    at: "Oct 03",
    icon: "✓",
    tone: "mint",
    title: "Payment received",
    sub: `€21,600.00 · INV-20251002 · Visa •• 4242`
  }, {
    at: "Oct 01",
    icon: "✉",
    tone: "info",
    title: "Invoice issued",
    sub: "INV-20251002 · due Oct 15"
  }, {
    at: "Sep 18",
    icon: "⟳",
    tone: "plum",
    title: "Seats added",
    sub: "+4 seats · prorated €1,440.00"
  }, {
    at: "Sep 03",
    icon: "✓",
    tone: "mint",
    title: "Payment received",
    sub: "€21,600.00 · INV-20250902"
  }].map((a, i, arr) => {
    const cc = {
      mint: ["var(--sp-accent-mint)", "rgba(0,184,148,.14)"],
      info: ["#1A73E8", "rgba(26,115,232,.12)"],
      plum: ["var(--sp-accent-plum)", "rgba(108,92,231,.14)"]
    }[a.tone];
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        display: "flex",
        gap: 14,
        padding: "14px 20px",
        borderBottom: i === arr.length - 1 ? "none" : "1px solid var(--sp-border)",
        alignItems: "center"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 28,
        height: 28,
        borderRadius: "50%",
        background: cc[1],
        color: cc[0],
        font: "600 13px/28px Roboto",
        textAlign: "center",
        flexShrink: 0
      }
    }, a.icon), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, a.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, a.sub)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, a.at));
  }))));
}

// ── MY SUBSCRIPTION (single, editable-lite) ────────────────────────────
function PSubscription({
  state,
  onUpgrade,
  onManage
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 300,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 240
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u27F3",
    title: "No active plan",
    body: "Choose a plan to start using Incedo CRM."
  }));
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "My plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 28
    }
  }, "Manage your plan, seats, and renewal preferences."), /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden",
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 56,
      height: 56,
      borderRadius: 14,
      background: "rgba(26,115,232,.14)",
      color: "#1A73E8",
      font: "700 22px/56px Roboto",
      textAlign: "center"
    }
  }, "G"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 20px/26px Roboto",
      color: "var(--sp-text)"
    }
  }, "Growth plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Monthly \xB7 auto-renews on ", ME.next, ", 2025")), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 26,
      color: "var(--sp-text)"
    }
  }, fmtMoney(ME.mrr), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, " / mo"))), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: onUpgrade
  }, "Upgrade")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24,
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "Seats"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 18px/24px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, ME.seats, " included \xB7 ", Math.round(ME.seats * ME.usage / 100), " in use"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      height: 8,
      borderRadius: 4,
      background: "var(--sp-surface-2)",
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: "100%",
      width: `${ME.usage}%`,
      background: "var(--sp-accent-mint)",
      borderRadius: 4
    }
  })), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      marginTop: 10,
      paddingLeft: 0
    },
    onClick: onManage
  }, "Manage seats \u2192")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(SideLabel, null, "Included features"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, ["Up to 50 seats", "Pipelines + automation", "Priority support", "API access"].map(f => /*#__PURE__*/React.createElement("div", {
    key: f,
    style: {
      display: "flex",
      gap: 8,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-accent-mint)"
    }
  }, "\u2713"), " ", f)))))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Cancellation"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, "You can cancel anytime. Your plan stays active until the end of the current billing period."), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Pause subscription"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      color: "#D93025"
    }
  }, "Cancel plan"))));
}

// ── BILLING (invoice history) ──────────────────────────────────────────
function PBilling({
  state,
  onOpen
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 220,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 6
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u20AC",
    title: "No invoices yet",
    body: "Your first invoice will appear here after your plan starts billing."
  }));
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Billing history"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 28
    }
  }, "All invoices for ", ME.customer, ". Click any row to view or download."), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    style: {
      background: "var(--sp-surface-2)"
    }
  }, ["Invoice", "Date", "Amount", "Status", "Paid on", ""].map((h, i) => /*#__PURE__*/React.createElement("th", {
    key: i,
    style: {
      padding: "14px 20px",
      textAlign: i === 2 ? "right" : "left",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, PORTAL_INVOICES.map((inv, i) => /*#__PURE__*/React.createElement("tr", {
    key: inv.id,
    onClick: () => onOpen(inv),
    style: {
      cursor: "pointer"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      font: "500 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, inv.id), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, inv.issued, ", 2025"), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      textAlign: "right",
      font: "500 14px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, fmtMoney(inv.amount)), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  })), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, inv.paidOn), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "16px 20px",
      textAlign: "right",
      borderBottom: i === PORTAL_INVOICES.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A7"
    })
  }, "PDF"))))))));
}

// ── INVOICE (portal version with "pay now" affordance) ─────────────────
function PInvoice({
  inv,
  onBack
}) {
  const subtotal = inv.amount;
  const tax = Math.round(subtotal * 0.21);
  const total = subtotal + tax;
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 20,
      display: "inline-block"
    }
  }, "\u2190 Back to billing"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 300px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "28px 32px",
      borderBottom: "2px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 32,
      height: 32,
      borderRadius: 8,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff",
      fontWeight: 700,
      fontSize: 14
    }
  }, "iC"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 14
    }
  }, "Incedo B.V. \xB7 Hoogoorddreef 9 \xB7 Amsterdam")), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.08em"
    }
  }, "Invoice"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 18px/24px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, inv.id), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "20px 32px",
      borderBottom: "1px solid var(--sp-border)",
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1fr",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Billed to"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, ME.customer)), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Issued"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, inv.issued, ", 2025")), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Total"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 24,
      marginTop: 6,
      color: "var(--sp-text)"
    }
  }, fmtMoney(total)))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 32
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "10px 0",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Growth plan \u2014 Monthly"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 14px/20px 'Roboto Mono',monospace"
    }
  }, fmtMoney(subtotal))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "10px 0"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "VAT (21%)"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text-muted)"
    }
  }, fmtMoney(tax))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "10px 0",
      borderTop: "2px solid var(--sp-border)",
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "700 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, "Total"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "700 15px/22px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(total))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 12
    }
  }, inv.status === "paid" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      background: "linear-gradient(135deg, rgba(0,184,148,.1), transparent)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-accent-mint)"
    }
  }, "\u2713 Paid in full"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, "Received on ", inv.paidOn || inv.due, ", 2025")), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Actions"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      display: "flex",
      flexDirection: "column",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A7"
    })
  }, "Download PDF"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    })
  }, "Email a copy"))))));
}

// ── PAYMENT METHOD ─────────────────────────────────────────────────────
function PPayment({
  state,
  onAdd
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 280,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 80
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25A3",
    title: "No payment method yet",
    body: "Add a card to keep your subscription running without interruption.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: onAdd
    }, "Add payment method")
  }));
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page-narrow"
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Payment methods"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 28
    }
  }, "We'll use your default method for automatic charges."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 12
    }
  }, PAYMENT_METHODS.map(pm => /*#__PURE__*/React.createElement(PCard, {
    key: pm.id,
    hover: true
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 18
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 64,
      height: 42,
      borderRadius: 6,
      flexShrink: 0,
      background: pm.brand === "Visa" ? "linear-gradient(135deg, #1a1f36 0%, #3d4666 100%)" : pm.brand === "Mastercard" ? "linear-gradient(135deg, #EB001B 0%, #F79E1B 100%)" : "linear-gradient(135deg, var(--sp-accent-plum) 0%, #1A73E8 100%)",
      color: "#fff",
      fontSize: 12,
      fontWeight: 700,
      textAlign: "center",
      lineHeight: "42px"
    }
  }, pm.brand), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, "\u2022\u2022\u2022\u2022 ", pm.last4), pm.default && /*#__PURE__*/React.createElement(PBadge, {
    variant: "info",
    dot: true
  }, "Default")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, pm.exp ? `Expires ${pm.exp}` : "SEPA Direct Debit", " \xB7 Added ", pm.added)), !pm.default && /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Make default"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Remove")))), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    }),
    onClick: onAdd,
    style: {
      alignSelf: "flex-start",
      marginTop: 8
    }
  }, "Add payment method")));
}

// ── UPGRADE / PLANS (simpler than admin version) ───────────────────────
function PUpgrade({
  state,
  onStart
}) {
  // Current plan starts as Growth (matches the "You're on Growth" copy in POverview & PSubscription).
  // Selection flow: click Upgrade/Downgrade → sets `pendingId`, which renders the card in a "Selected" state
  // and surfaces a sticky confirmation bar at the bottom of the page. Confirm = commit, Cancel = clear.
  const [currentId, setCurrentId] = React.useState("growth");
  const [pendingId, setPendingId] = React.useState(null);
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 280,
    h: 34
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3,1fr)",
      gap: 16
    }
  }, [0, 1, 2].map(i => /*#__PURE__*/React.createElement(PCard, {
    key: i
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 240
  })))));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    className: "sp-page"
  }, /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25C7",
    title: "No plans available",
    body: "Contact your account manager to discuss plan options."
  }));
  const currentPlan = PLANS.find(p => p.id === currentId);
  const pendingPlan = pendingId ? PLANS.find(p => p.id === pendingId) : null;
  const isDowngrade = pendingPlan && pendingPlan.price < currentPlan.price;
  const handleConfirm = () => {
    setCurrentId(pendingId);
    setPendingId(null);
    onStart(pendingPlan);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "sp-page",
    style: {
      paddingBottom: pendingPlan ? 120 : undefined
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 32px/38px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em",
      textAlign: "center"
    }
  }, "Pick the plan that fits"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 15px/22px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 8,
      textAlign: "center",
      marginBottom: 32
    }
  }, "You're on ", /*#__PURE__*/React.createElement("b", {
    style: {
      color: "var(--sp-text)"
    }
  }, currentPlan.name), ". Switch any time, prorated automatically."), /*#__PURE__*/React.createElement("div", {
    className: "sp-plans-grid",
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: 16
    }
  }, PLANS.map(p => {
    const isCurrent = p.id === currentId;
    const isPending = p.id === pendingId;
    const thisDowngrade = p.price < currentPlan.price;

    // Visual priority: pending (selected) > current > default
    const borderColor = isPending ? p.color : "var(--sp-border)";
    const outline = isPending ? `2px solid ${p.color}` : isCurrent ? "2px solid var(--sp-accent-mint)" : "none";
    return /*#__PURE__*/React.createElement(PCard, {
      key: p.id,
      hover: true,
      style: {
        position: "relative",
        overflow: "hidden",
        borderTop: `3px solid ${p.color}`,
        outline,
        outlineOffset: isPending || isCurrent ? -2 : 0,
        background: isPending ? "color-mix(in oklab, " + (p.color.startsWith("var") ? "#2ECC9B" : p.color) + " 6%, var(--sp-surface))" : "var(--sp-surface)",
        transition: "outline 120ms, background 120ms"
      }
    }, isPending && /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: 12,
        right: 12
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        padding: "4px 10px",
        borderRadius: 999,
        background: p.color,
        color: "#fff",
        font: "600 11px/14px Roboto",
        letterSpacing: "0.02em"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 6,
        height: 6,
        borderRadius: "50%",
        background: "#fff"
      }
    }), "Selected")), !isPending && isCurrent && /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: 12,
        right: 12
      }
    }, /*#__PURE__*/React.createElement(PBadge, {
      variant: "mint",
      dot: true
    }, "Current plan")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "600 18px/24px Roboto",
        color: "var(--sp-text)"
      }
    }, p.name), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "baseline",
        gap: 4,
        marginTop: 12
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "sp-display",
      style: {
        fontSize: 38,
        color: "var(--sp-text)"
      }
    }, fmtMoney(p.price)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 13px/18px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "/ month")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, p.seats ? `up to ${p.seats} seats` : "unlimited seats"), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 18,
        display: "flex",
        flexDirection: "column",
        gap: 8
      }
    }, p.feats.map((f, i) => /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        display: "flex",
        gap: 8,
        font: "400 13px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        color: p.color
      }
    }, "\u2713"), " ", f))), /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 22
      }
    }, isCurrent ? /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      style: {
        width: "100%"
      },
      disabled: true
    }, "Current plan") : isPending ? /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      style: {
        width: "100%"
      },
      onClick: () => setPendingId(null)
    }, "\u2713 Selected \u2014 tap to deselect") : /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      style: {
        width: "100%",
        background: p.color
      },
      onClick: () => setPendingId(p.id)
    }, thisDowngrade ? "Downgrade" : "Upgrade", " to ", p.name)));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "center",
      marginTop: 32,
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "All prices exclude VAT. You'll see a prorated credit for unused days on your current plan."), pendingPlan && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "sticky",
      bottom: 16,
      marginTop: 24,
      background: "var(--sp-surface)",
      border: "1px solid var(--sp-border)",
      borderRadius: 12,
      boxShadow: "0 12px 32px rgba(17,24,39,0.12), 0 2px 6px rgba(17,24,39,0.06)",
      padding: "14px 18px",
      display: "flex",
      alignItems: "center",
      gap: 16,
      flexWrap: "wrap",
      zIndex: 5
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 220
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, isDowngrade ? "Downgrade" : "Upgrade", " from ", /*#__PURE__*/React.createElement("b", null, currentPlan.name), " to ", /*#__PURE__*/React.createElement("b", {
    style: {
      color: pendingPlan.color
    }
  }, pendingPlan.name), "?"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, isDowngrade ? `New rate: ${fmtMoney(pendingPlan.price)}/mo. Credit will be applied to your next invoice.` : `New rate: ${fmtMoney(pendingPlan.price)}/mo. Prorated charge issued today.`)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    onClick: () => setPendingId(null)
  }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    style: {
      background: pendingPlan.color
    },
    onClick: handleConfirm
  }, "Confirm ", isDowngrade ? "downgrade" : "upgrade"))));
}
Object.assign(window, {
  PortalTopBar,
  PortalBottomTabs,
  POverview,
  PSubscription,
  PBilling,
  PInvoice,
  PPayment,
  PUpgrade,
  ME,
  PORTAL_INVOICES
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/portal_Screens.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Data.jsx
try { (() => {
// Subscriptions prototype — fixtures

const SUBS = [{
  id: "sub_001",
  customer: "Acme Holdings",
  plan: "Enterprise",
  seats: 120,
  mrr: 49900,
  status: "active",
  next: "Nov 01",
  since: "2023-02-14",
  owner: "Anna Krause",
  lastInv: "paid",
  usage: 78
}, {
  id: "sub_002",
  customer: "Orbit Labs",
  plan: "Growth",
  seats: 42,
  mrr: 21600,
  status: "active",
  next: "Oct 28",
  since: "2024-01-08",
  owner: "Bram de Vries",
  lastInv: "paid",
  usage: 92
}, {
  id: "sub_003",
  customer: "Northwind GmbH",
  plan: "Enterprise",
  seats: 85,
  mrr: 38400,
  status: "past_due",
  next: "Oct 18",
  since: "2022-11-03",
  owner: "Anna Krause",
  lastInv: "failed",
  usage: 64
}, {
  id: "sub_004",
  customer: "Peregrine AI",
  plan: "Starter",
  seats: 8,
  mrr: 4900,
  status: "trialing",
  next: "Oct 31",
  since: "2025-10-08",
  owner: "Chiara Romano",
  lastInv: "—",
  usage: 21
}, {
  id: "sub_005",
  customer: "Hanzeborg NV",
  plan: "Growth",
  seats: 28,
  mrr: 16800,
  status: "active",
  next: "Nov 02",
  since: "2024-06-19",
  owner: "Bram de Vries",
  lastInv: "paid",
  usage: 55
}, {
  id: "sub_006",
  customer: "Lumen Studios",
  plan: "Growth",
  seats: 19,
  mrr: 11400,
  status: "canceled",
  next: "—",
  since: "2023-09-27",
  owner: "Chiara Romano",
  lastInv: "paid",
  usage: 0
}, {
  id: "sub_007",
  customer: "Meridian Fintech",
  plan: "Enterprise",
  seats: 260,
  mrr: 89200,
  status: "active",
  next: "Nov 07",
  since: "2021-04-02",
  owner: "Anna Krause",
  lastInv: "paid",
  usage: 88
}, {
  id: "sub_008",
  customer: "Polder & Co",
  plan: "Starter",
  seats: 4,
  mrr: 2400,
  status: "past_due",
  next: "Oct 14",
  since: "2025-03-22",
  owner: "Bram de Vries",
  lastInv: "failed",
  usage: 43
}, {
  id: "sub_009",
  customer: "Kairos Mobility",
  plan: "Growth",
  seats: 36,
  mrr: 18900,
  status: "active",
  next: "Oct 30",
  since: "2024-08-11",
  owner: "Chiara Romano",
  lastInv: "paid",
  usage: 69
}, {
  id: "sub_010",
  customer: "Thornebridge LLP",
  plan: "Enterprise",
  seats: 145,
  mrr: 58000,
  status: "paused",
  next: "—",
  since: "2022-07-15",
  owner: "Anna Krause",
  lastInv: "paid",
  usage: 0
}];
const INVOICES = [{
  id: "INV-20251001",
  customer: "Acme Holdings",
  amount: 49900,
  status: "paid",
  issued: "Oct 01",
  due: "Oct 15"
}, {
  id: "INV-20251002",
  customer: "Orbit Labs",
  amount: 21600,
  status: "paid",
  issued: "Oct 01",
  due: "Oct 15"
}, {
  id: "INV-20251003",
  customer: "Northwind GmbH",
  amount: 38400,
  status: "overdue",
  issued: "Oct 01",
  due: "Oct 15",
  attempts: 3
}, {
  id: "INV-20251004",
  customer: "Polder & Co",
  amount: 2400,
  status: "overdue",
  issued: "Oct 01",
  due: "Oct 15",
  attempts: 2
}, {
  id: "INV-20251005",
  customer: "Hanzeborg NV",
  amount: 16800,
  status: "paid",
  issued: "Oct 02",
  due: "Oct 16"
}, {
  id: "INV-20251006",
  customer: "Meridian Fintech",
  amount: 89200,
  status: "paid",
  issued: "Oct 07",
  due: "Oct 21"
}, {
  id: "INV-20251007",
  customer: "Peregrine AI",
  amount: 0,
  status: "draft",
  issued: "—",
  due: "—"
}, {
  id: "INV-20251008",
  customer: "Kairos Mobility",
  amount: 18900,
  status: "pending",
  issued: "Oct 10",
  due: "Oct 24"
}, {
  id: "INV-20251009",
  customer: "Acme Holdings",
  amount: 4200,
  status: "draft",
  issued: "—",
  due: "—"
}];
const PLANS = [{
  id: "starter",
  name: "Starter",
  price: 4900,
  interval: "mo",
  seats: 10,
  color: "#1A73E8",
  subs: 38,
  mrr: 186200,
  feats: ["Up to 10 seats", "CRM core", "Email support"]
}, {
  id: "growth",
  name: "Growth",
  price: 19900,
  interval: "mo",
  seats: 50,
  color: "var(--sp-accent-mint)",
  subs: 94,
  mrr: 1870600,
  feats: ["Up to 50 seats", "Pipelines + automation", "Priority support", "API access"]
}, {
  id: "enterprise",
  name: "Enterprise",
  price: 89900,
  interval: "mo",
  seats: null,
  color: "var(--sp-accent-plum)",
  subs: 52,
  mrr: 4674800,
  feats: ["Unlimited seats", "SSO + SAML", "Dedicated CSM", "99.95% SLA", "Custom data residency"]
}];
const PAYMENT_METHODS = [{
  id: "pm_1",
  kind: "card",
  brand: "Visa",
  last4: "4242",
  exp: "09/27",
  default: true,
  added: "Feb 2024"
}, {
  id: "pm_2",
  kind: "card",
  brand: "Mastercard",
  last4: "8822",
  exp: "03/26",
  default: false,
  added: "Mar 2024"
}, {
  id: "pm_3",
  kind: "sepa",
  brand: "SEPA DD",
  last4: "8411",
  exp: null,
  default: false,
  added: "Aug 2024"
}];
const DUNNING = [{
  id: "INV-20251003",
  customer: "Northwind GmbH",
  amount: 38400,
  attempts: 3,
  nextRetry: "Oct 20",
  reason: "insufficient_funds",
  age: 10,
  severity: "high",
  owner: "Anna Krause"
}, {
  id: "INV-20251004",
  customer: "Polder & Co",
  amount: 2400,
  attempts: 2,
  nextRetry: "Oct 19",
  reason: "expired_card",
  age: 10,
  severity: "medium",
  owner: "Bram de Vries"
}, {
  id: "INV-20250912",
  customer: "Hanzeborg NV",
  amount: 16800,
  attempts: 1,
  nextRetry: "Oct 19",
  reason: "do_not_honor",
  age: 4,
  severity: "low",
  owner: "Chiara Romano"
}, {
  id: "INV-20250908",
  customer: "Kairos Mobility",
  amount: 18900,
  attempts: 4,
  nextRetry: "—",
  reason: "authentication",
  age: 22,
  severity: "high",
  owner: "Chiara Romano"
}];
const ACTIVITY = [{
  id: 1,
  at: "2m ago",
  icon: "✓",
  tone: "mint",
  title: "Payment received — €49,900.00",
  sub: "Acme Holdings · INV-20251001 · Visa •• 4242"
}, {
  id: 2,
  at: "14m ago",
  icon: "⟳",
  tone: "plum",
  title: "Subscription upgraded",
  sub: "Orbit Labs · Growth → Enterprise · +42 seats"
}, {
  id: 3,
  at: "1h ago",
  icon: "✕",
  tone: "warm",
  title: "Payment failed — €38,400.00",
  sub: "Northwind GmbH · insufficient_funds · attempt 3 of 5"
}, {
  id: 4,
  at: "2h ago",
  icon: "✎",
  tone: "info",
  title: "Invoice sent",
  sub: "INV-20251008 · Kairos Mobility · €18,900.00"
}, {
  id: 5,
  at: "4h ago",
  icon: "▣",
  tone: "info",
  title: "Payment method added",
  sub: "Lumen Studios · Mastercard •• 1121"
}, {
  id: 6,
  at: "Yday",
  icon: "⊘",
  tone: "muted",
  title: "Trial ending in 3 days",
  sub: "Peregrine AI · Starter plan"
}];

// Monthly MRR history (12 months)
const MRR_SERIES = [412, 438, 462, 481, 495, 528, 561, 598, 624, 651, 689, 728]; // in k€

// Tweak defaults (EDITMODE-BEGIN/END block in root HTML)
Object.assign(window, {
  SUBS,
  INVOICES,
  PLANS,
  PAYMENT_METHODS,
  DUNNING,
  ACTIVITY,
  MRR_SERIES
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Data.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Primitives.jsx
try { (() => {
// Subscriptions prototype — primitives (dark-aware, token-driven)
// All colors read from CSS variables in sub_tokens.css

const {
  useState: useStateP,
  useEffect: useEffectP,
  useRef: useRefP
} = React;

// ── Money formatter ────────────────────────────────────────────────────
const fmtMoney = (cents, cur = "EUR") => {
  const sign = cents < 0 ? "-" : "";
  const abs = Math.abs(cents) / 100;
  const s = new Intl.NumberFormat("en-US", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  }).format(abs);
  return `${sign}${cur === "EUR" ? "€" : "$"}${s}`;
};
const fmtShortMoney = cents => {
  const abs = Math.abs(cents) / 100;
  if (abs >= 1000) return `€${(abs / 1000).toFixed(1)}k`;
  return `€${abs.toFixed(0)}`;
};

// ── Icon ────────────────────────────────────────────────────────────────
function Ico({
  g,
  size = 16,
  style
}) {
  return /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: size,
      lineHeight: 1,
      display: "inline-flex",
      ...style
    }
  }, g);
}

// ── Button ──────────────────────────────────────────────────────────────
function PButton({
  variant = "primary",
  size = "md",
  leading,
  trailing,
  children,
  onClick,
  disabled,
  style
}) {
  const sizes = {
    sm: {
      padding: "6px 10px",
      fontSize: 12,
      lineHeight: "16px",
      borderRadius: 6,
      gap: 6
    },
    md: {
      padding: "10px 14px",
      fontSize: 13,
      lineHeight: "18px",
      borderRadius: 8,
      gap: 8
    },
    lg: {
      padding: "14px 20px",
      fontSize: 15,
      lineHeight: "20px",
      borderRadius: 10,
      gap: 10
    }
  };
  const base = {
    ...sizes[size],
    border: "none",
    fontFamily: "inherit",
    fontWeight: 500,
    cursor: disabled ? "not-allowed" : "pointer",
    display: "inline-flex",
    alignItems: "center",
    justifyContent: "center",
    transition: "all 160ms cubic-bezier(.4,0,.2,1)",
    whiteSpace: "nowrap"
  };
  const variants = {
    primary: {
      background: "#1A73E8",
      color: "#fff"
    },
    mint: {
      background: "var(--sp-accent-mint)",
      color: "#fff"
    },
    warm: {
      background: "var(--sp-accent-warm)",
      color: "#fff"
    },
    plum: {
      background: "var(--sp-accent-plum)",
      color: "#fff"
    },
    secondary: {
      background: "var(--sp-surface)",
      color: "var(--sp-text)",
      boxShadow: "inset 0 0 0 1px var(--sp-border)"
    },
    ghost: {
      background: "transparent",
      color: "var(--sp-text)"
    },
    danger: {
      background: "#D93025",
      color: "#fff"
    }
  };
  const disabledStyle = disabled ? {
    background: "var(--sp-surface-2)",
    color: "var(--sp-text-subtle)",
    boxShadow: "none"
  } : {};
  return /*#__PURE__*/React.createElement("button", {
    style: {
      ...base,
      ...variants[variant],
      ...disabledStyle,
      ...style
    },
    onClick: disabled ? undefined : onClick,
    disabled: disabled
  }, leading && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex"
    }
  }, leading), children, trailing && /*#__PURE__*/React.createElement("span", {
    style: {
      display: "inline-flex"
    }
  }, trailing));
}

// ── Input ───────────────────────────────────────────────────────────────
function PInput({
  label,
  value,
  onChange,
  placeholder,
  leading,
  trailing,
  error,
  disabled,
  type = "text",
  style,
  compact
}) {
  const [focus, setFocus] = useStateP(false);
  const borderColor = error ? "#D93025" : focus ? "#1A73E8" : "var(--sp-border)";
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 4,
      ...style
    }
  }, label && /*#__PURE__*/React.createElement("label", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      padding: compact ? "6px 10px" : "10px 12px",
      border: `1px solid ${borderColor}`,
      borderRadius: 8,
      background: disabled ? "var(--sp-surface-2)" : "var(--sp-surface)",
      boxShadow: focus && !error ? "var(--sp-glow-primary)" : "none",
      transition: "all 160ms cubic-bezier(.4,0,.2,1)"
    }
  }, leading && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)"
    }
  }, leading), /*#__PURE__*/React.createElement("input", {
    type: type,
    value: value ?? "",
    placeholder: placeholder,
    disabled: disabled,
    onChange: e => onChange?.(e.target.value),
    onFocus: () => setFocus(true),
    onBlur: () => setFocus(false),
    style: {
      border: "none",
      outline: "none",
      background: "transparent",
      flex: 1,
      minWidth: 0,
      font: "400 14px/20px Roboto",
      color: disabled ? "var(--sp-text-subtle)" : "var(--sp-text)"
    }
  }), trailing && /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)"
    }
  }, trailing)), error && /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "#D93025"
    }
  }, error));
}

// ── Badge ───────────────────────────────────────────────────────────────
function PBadge({
  variant = "default",
  dot,
  children,
  style
}) {
  const palette = {
    default: ["var(--sp-surface-2)", "var(--sp-text)"],
    mint: ["rgba(0,184,148,.14)", "var(--sp-accent-mint)"],
    warm: ["rgba(255,122,89,.14)", "var(--sp-accent-warm)"],
    plum: ["rgba(108,92,231,.14)", "var(--sp-accent-plum)"],
    amber: ["rgba(244,180,0,.18)", "#B06000"],
    info: ["rgba(26,115,232,.12)", "#1A73E8"],
    error: ["rgba(217,48,37,.12)", "#D93025"],
    muted: ["transparent", "var(--sp-text-muted)"]
  };
  const [bg, fg] = palette[variant];
  return /*#__PURE__*/React.createElement("span", {
    style: {
      background: bg,
      color: fg,
      padding: "3px 9px",
      borderRadius: 999,
      font: "500 11px/14px Roboto",
      display: "inline-flex",
      alignItems: "center",
      gap: 6,
      whiteSpace: "nowrap",
      ...style
    }
  }, dot && /*#__PURE__*/React.createElement("span", {
    style: {
      width: 6,
      height: 6,
      borderRadius: "50%",
      background: fg,
      flexShrink: 0
    }
  }), children);
}

// ── Avatar ──────────────────────────────────────────────────────────────
function PAvatar({
  name,
  size = 32,
  bg
}) {
  const palette = ["#1A73E8", "var(--sp-accent-mint)", "var(--sp-accent-plum)", "var(--sp-accent-warm)", "#0EA5E9", "#EC4899"];
  const idx = (name || "?").charCodeAt(0) % palette.length;
  const color = bg || palette[idx];
  const initials = (name || "?").split(" ").slice(0, 2).map(s => s[0]?.toUpperCase()).join("");
  return /*#__PURE__*/React.createElement("span", {
    style: {
      width: size,
      height: size,
      borderRadius: "50%",
      background: color,
      color: "#fff",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      fontFamily: "Roboto",
      fontWeight: 700,
      fontSize: Math.round(size * 0.4),
      flexShrink: 0,
      letterSpacing: "-0.02em"
    }
  }, initials);
}

// ── Card ────────────────────────────────────────────────────────────────
function PCard({
  children,
  pad = 20,
  style,
  hover
}) {
  const [h, setH] = useStateP(false);
  return /*#__PURE__*/React.createElement("div", {
    onMouseEnter: () => setH(true),
    onMouseLeave: () => setH(false),
    style: {
      background: "var(--sp-surface)",
      borderRadius: "var(--sp-r-lg)",
      padding: pad,
      boxShadow: hover && h ? "var(--sp-shadow-2)" : "var(--sp-shadow-1)",
      transition: "box-shadow 160ms, transform 160ms",
      ...style
    }
  }, children);
}

// ── Tooltip ─────────────────────────────────────────────────────────────
function PTooltip({
  text,
  children
}) {
  const [vis, setVis] = useStateP(false);
  return /*#__PURE__*/React.createElement("span", {
    style: {
      position: "relative",
      display: "inline-flex"
    },
    onMouseEnter: () => setVis(true),
    onMouseLeave: () => setVis(false)
  }, children, vis && text && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      bottom: "calc(100% + 6px)",
      left: "50%",
      transform: "translateX(-50%)",
      background: "var(--sp-text)",
      color: "var(--sp-surface)",
      padding: "4px 8px",
      borderRadius: 6,
      font: "500 11px/14px Roboto",
      whiteSpace: "nowrap",
      pointerEvents: "none",
      zIndex: 50
    }
  }, text));
}

// ── Skeleton ────────────────────────────────────────────────────────────
function PSkeleton({
  w = "100%",
  h = 16,
  r = 6,
  style
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "sp-shine",
    style: {
      display: "inline-block",
      width: w,
      height: h,
      background: "var(--sp-surface-2)",
      borderRadius: r,
      ...style
    }
  });
}

// ── Segmented ───────────────────────────────────────────────────────────
function PSegmented({
  options,
  value,
  onChange,
  size = "md"
}) {
  const pad = size === "sm" ? "5px 10px" : "8px 14px";
  const fs = size === "sm" ? 12 : 13;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "inline-flex",
      padding: 3,
      borderRadius: 10,
      background: "var(--sp-surface-2)",
      gap: 2
    }
  }, options.map(o => {
    const on = o.value === value;
    return /*#__PURE__*/React.createElement("span", {
      key: o.value,
      onClick: () => onChange(o.value),
      style: {
        padding: pad,
        borderRadius: 7,
        font: `500 ${fs}px/18px Roboto`,
        cursor: "pointer",
        userSelect: "none",
        color: on ? "var(--sp-text)" : "var(--sp-text-muted)",
        background: on ? "var(--sp-surface)" : "transparent",
        boxShadow: on ? "var(--sp-shadow-1)" : "none",
        transition: "all 150ms"
      }
    }, o.label);
  }));
}

// ── Spark bar ───────────────────────────────────────────────────────────
function PSparkBars({
  data,
  height = 36,
  width = 120,
  color = "var(--sp-accent-mint)"
}) {
  const max = Math.max(...data, 1);
  const bw = width / data.length - 2;
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: `0 0 ${width} ${height}`,
    width: width,
    height: height
  }, data.map((v, i) => {
    const h = v / max * (height - 4);
    return /*#__PURE__*/React.createElement("rect", {
      key: i,
      x: i * (bw + 2),
      y: height - h - 2,
      width: bw,
      height: h,
      rx: 1.5,
      fill: color,
      opacity: 0.2 + 0.8 * (v / max)
    });
  }));
}
Object.assign(window, {
  fmtMoney,
  fmtShortMoney,
  Ico,
  PButton,
  PInput,
  PBadge,
  PAvatar,
  PCard,
  PTooltip,
  PSkeleton,
  PSegmented,
  PSparkBars
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Primitives.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Screens1.jsx
try { (() => {
// Subscriptions prototype — dashboard + subscriptions list

const {
  useState: useSt1
} = React;

// ── Helpers shared across screens ──────────────────────────────────────
function StatusChip({
  s
}) {
  const map = {
    active: {
      v: "mint",
      label: "Active",
      dot: true
    },
    trialing: {
      v: "amber",
      label: "Trialing",
      dot: true
    },
    past_due: {
      v: "warm",
      label: "Past due",
      dot: true
    },
    canceled: {
      v: "muted",
      label: "Canceled",
      dot: false
    },
    paused: {
      v: "muted",
      label: "Paused",
      dot: false
    },
    paid: {
      v: "mint",
      label: "Paid",
      dot: true
    },
    overdue: {
      v: "warm",
      label: "Overdue",
      dot: true
    },
    pending: {
      v: "amber",
      label: "Pending",
      dot: true
    },
    draft: {
      v: "muted",
      label: "Draft",
      dot: false
    },
    failed: {
      v: "warm",
      label: "Failed",
      dot: true
    }
  };
  const m = map[s] || {
    v: "default",
    label: s
  };
  return /*#__PURE__*/React.createElement(PBadge, {
    variant: m.v,
    dot: m.dot
  }, m.label);
}
function ScreenHeader({
  title,
  subtitle,
  actions,
  kpi
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-end",
      gap: 24,
      marginBottom: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, title), subtitle && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 14px/20px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, subtitle)), kpi, actions && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8
    }
  }, actions));
}
function EmptyState({
  icon = "◌",
  title,
  body,
  cta
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "64px 24px",
      textAlign: "center",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 12,
      borderRadius: 14,
      border: "1px dashed var(--sp-border)",
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 56,
      height: 56,
      borderRadius: "50%",
      background: "var(--sp-surface-2)",
      color: "var(--sp-text-muted)",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      fontSize: 24
    }
  }, icon), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, title), body && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      maxWidth: 380
    }
  }, body), cta && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6
    }
  }, cta));
}
function LoadingRows({
  count = 6
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, Array.from({
    length: count
  }).map((_, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 16,
      padding: "14px 16px",
      borderBottom: "1px solid var(--sp-border)",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: 28,
    h: 28,
    r: 14
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 160,
    h: 14
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 80,
    h: 12
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 70,
    h: 20,
    r: 10
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: 90,
    h: 14
  }))));
}
function ErrorState({
  onRetry
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "48px 24px",
      textAlign: "center",
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 10,
      borderRadius: 14,
      background: "var(--sp-surface)",
      border: "1px solid rgba(217,48,37,.2)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      borderRadius: "50%",
      background: "rgba(217,48,37,.12)",
      color: "#D93025",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      fontSize: 22
    }
  }, "\u26A0"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Couldn't load this view"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "The billing service returned an error. Retry, or reach out to ops."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: onRetry
  }, "Retry"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary"
  }, "Open status page")));
}

// ── DASHBOARD ──────────────────────────────────────────────────────────
function DashboardP({
  state,
  onNavigate,
  onCreate
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Revenue overview",
    subtitle: "Live from Stripe \xB7 syncing"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 16,
      marginBottom: 16
    }
  }, Array.from({
    length: 4
  }).map((_, i) => /*#__PURE__*/React.createElement(PCard, {
    key: i
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "60%",
    h: 12
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "80%",
    h: 28
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "40%",
    h: 10
  })))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "30%",
    h: 14
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 20
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 180
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Revenue overview"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Revenue overview",
    subtitle: "As of today \xB7 synced from Stripe 2 min ago",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Export"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      }),
      onClick: onCreate
    }, "New subscription"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1fr 1fr 1fr",
      gap: 16,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden",
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/16px Roboto",
      textTransform: "uppercase",
      letterSpacing: "0.08em",
      opacity: .85
    }
  }, "Monthly recurring revenue"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 38,
      lineHeight: "46px",
      marginTop: 6
    }
  }, "\u20AC728,140"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      marginTop: 6,
      font: "500 12px/16px Roboto"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      background: "rgba(255,255,255,.18)",
      padding: "2px 8px",
      borderRadius: 10
    }
  }, "\u25B2 5.6%"), /*#__PURE__*/React.createElement("span", {
    style: {
      opacity: .85
    }
  }, "vs last month"))), /*#__PURE__*/React.createElement("div", {
    style: {
      opacity: .3,
      fontSize: 42,
      letterSpacing: "-0.04em",
      fontFamily: "'Roboto Mono',monospace"
    }
  }, "MRR")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20
    }
  }, /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 400 60",
    width: "100%",
    height: "60",
    preserveAspectRatio: "none"
  }, /*#__PURE__*/React.createElement("defs", null, /*#__PURE__*/React.createElement("linearGradient", {
    id: "sparkFill",
    x1: "0",
    y1: "0",
    x2: "0",
    y2: "1"
  }, /*#__PURE__*/React.createElement("stop", {
    offset: "0%",
    stopColor: "#fff",
    stopOpacity: ".35"
  }), /*#__PURE__*/React.createElement("stop", {
    offset: "100%",
    stopColor: "#fff",
    stopOpacity: "0"
  }))), (() => {
    const data = MRR_SERIES;
    const max = Math.max(...data);
    const min = Math.min(...data);
    const norm = data.map((v, i) => [i * (400 / (data.length - 1)), 60 - (v - min) / (max - min) * 54 - 3]);
    const path = norm.map((p, i) => (i ? "L" : "M") + p[0].toFixed(1) + " " + p[1].toFixed(1)).join(" ");
    const area = path + ` L 400 60 L 0 60 Z`;
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("path", {
      d: area,
      fill: "url(#sparkFill)"
    }), /*#__PURE__*/React.createElement("path", {
      d: path,
      stroke: "#fff",
      strokeWidth: "2",
      fill: "none"
    }), norm.map((p, i) => /*#__PURE__*/React.createElement("circle", {
      key: i,
      cx: p[0],
      cy: p[1],
      r: i === norm.length - 1 ? 3.5 : 0,
      fill: "#fff"
    })));
  })()), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      font: "400 10px/14px Roboto",
      opacity: .75,
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("span", null, "Nov '24"), /*#__PURE__*/React.createElement("span", null, "Feb"), /*#__PURE__*/React.createElement("span", null, "May"), /*#__PURE__*/React.createElement("span", null, "Aug"), /*#__PURE__*/React.createElement("span", null, "Today"))))), /*#__PURE__*/React.createElement(KPICard, {
    label: "Active subscriptions",
    value: "248",
    delta: "+12",
    deltaTone: "mint",
    spark: [42, 48, 51, 55, 58, 62, 66, 70, 73, 78, 82, 86],
    accent: "var(--sp-accent-mint)"
  }), /*#__PURE__*/React.createElement(KPICard, {
    label: "Net revenue retention",
    value: "114%",
    delta: "+2.1%",
    deltaTone: "mint",
    spark: [108, 110, 109, 112, 113, 111, 112, 114, 115, 113, 114, 114],
    accent: "var(--sp-accent-plum)"
  }), /*#__PURE__*/React.createElement(KPICard, {
    label: "Revenue at risk",
    value: "\u20AC57,700",
    delta: "4 invoices",
    deltaTone: "warm",
    spark: [10, 12, 14, 18, 22, 30, 38, 44, 48, 52, 55, 57],
    accent: "var(--sp-accent-warm)",
    onClick: () => onNavigate("dunning")
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1.4fr 1fr",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 20px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Recently active subscriptions"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: () => onNavigate("subscriptions")
  }, "View all \u2192")), /*#__PURE__*/React.createElement("div", null, SUBS.slice(0, 6).map((s, i) => /*#__PURE__*/React.createElement("div", {
    key: s.id,
    style: {
      padding: "12px 20px",
      display: "flex",
      alignItems: "center",
      gap: 14,
      borderBottom: i === 5 ? "none" : "1px solid var(--sp-border)",
      cursor: "pointer"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent",
    onClick: () => onNavigate("sub-detail", s)
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: s.customer,
    size: 32
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, s.customer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, s.plan, " \xB7 ", s.seats, " seats")), /*#__PURE__*/React.createElement("div", {
    className: "sp-money",
    style: {
      font: "500 14px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      minWidth: 80,
      textAlign: "right"
    }
  }, fmtMoney(s.mrr), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)",
      fontSize: 11
    }
  }, "/mo")), /*#__PURE__*/React.createElement(StatusChip, {
    s: s.status
  }))))), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 20px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Activity"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Filter")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "6px 16px 16px"
    }
  }, ACTIVITY.map((a, i) => {
    const tone = {
      mint: ["var(--sp-accent-mint)", "rgba(0,184,148,.14)"],
      warm: ["var(--sp-accent-warm)", "rgba(255,122,89,.14)"],
      plum: ["var(--sp-accent-plum)", "rgba(108,92,231,.14)"],
      info: ["#1A73E8", "rgba(26,115,232,.12)"],
      muted: ["var(--sp-text-muted)", "var(--sp-surface-2)"]
    }[a.tone];
    return /*#__PURE__*/React.createElement("div", {
      key: a.id,
      style: {
        display: "flex",
        gap: 12,
        padding: "10px 4px",
        alignItems: "flex-start"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 26,
        height: 26,
        borderRadius: "50%",
        flexShrink: 0,
        background: tone[1],
        color: tone[0],
        font: "600 12px/26px Roboto",
        textAlign: "center",
        marginTop: 1
      }
    }, a.icon), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 13px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, a.title), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/15px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 1
      }
    }, a.sub)), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-subtle)",
        whiteSpace: "nowrap"
      }
    }, a.at));
  })))));
}
function KPICard({
  label,
  value,
  delta,
  deltaTone = "mint",
  spark,
  accent,
  onClick
}) {
  const deltaColor = {
    mint: "var(--sp-accent-mint)",
    warm: "var(--sp-accent-warm)",
    plum: "var(--sp-accent-plum)"
  }[deltaTone];
  const arrow = deltaTone === "warm" ? "▲" : deltaTone === "mint" ? "▲" : "●";
  return /*#__PURE__*/React.createElement(PCard, {
    hover: true,
    style: {
      cursor: onClick ? "pointer" : "default"
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClick
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/16px Roboto",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      color: "var(--sp-text-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 26,
      lineHeight: "32px",
      marginTop: 6,
      color: "var(--sp-text)"
    }
  }, value), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8,
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 11px/14px Roboto",
      color: deltaColor
    }
  }, arrow, " ", delta)), spark && /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 8,
      height: 30
    }
  }, /*#__PURE__*/React.createElement(PSparkBars, {
    data: spark,
    height: 30,
    width: 180,
    color: accent
  }))));
}

// ── SUBSCRIPTIONS LIST ─────────────────────────────────────────────────
function SubscriptionsP({
  state,
  onOpen,
  onCreate
}) {
  const [status, setStatus] = useSt1("all");
  const [q, setQ] = useSt1("");
  const filtered = SUBS.filter(s => (status === "all" || s.status === status) && (!q || s.customer.toLowerCase().includes(q.toLowerCase()) || s.id.includes(q)));
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Subscriptions"
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 8
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Subscriptions"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Subscriptions"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u27F3",
    title: "No subscriptions yet",
    body: "When a deal closes-won, a subscription is created automatically. Or start one manually from a quote.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      }),
      onClick: onCreate
    }, "Create subscription")
  }));
  const totalMrr = SUBS.filter(s => s.status === "active").reduce((a, s) => a + s.mrr, 0);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Subscriptions",
    subtitle: `${SUBS.length} total · ${SUBS.filter(s => s.status === "active").length} active · ${fmtMoney(totalMrr)}/mo MRR`,
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Export CSV"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      }),
      onClick: onCreate
    }, "New subscription"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    value: status,
    onChange: setStatus,
    size: "sm",
    options: [{
      value: "all",
      label: `All (${SUBS.length})`
    }, {
      value: "active",
      label: `Active (${SUBS.filter(s => s.status === "active").length})`
    }, {
      value: "trialing",
      label: `Trialing (${SUBS.filter(s => s.status === "trialing").length})`
    }, {
      value: "past_due",
      label: `Past due (${SUBS.filter(s => s.status === "past_due").length})`
    }, {
      value: "canceled",
      label: `Canceled (${SUBS.filter(s => s.status === "canceled").length})`
    }, {
      value: "paused",
      label: `Paused (${SUBS.filter(s => s.status === "paused").length})`
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    value: q,
    onChange: setQ,
    placeholder: "Search customer or sub ID\u2026",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2315"
    }),
    style: {
      width: 260
    }
  }), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u29E9"
    })
  }, "Filters")), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    style: {
      background: "var(--sp-surface-2)"
    }
  }, ["Customer", "Plan", "Seats", "MRR", "Status", "Next charge", "Owner", ""].map((h, i) => /*#__PURE__*/React.createElement("th", {
    key: i,
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      textAlign: i === 2 || i === 3 ? "right" : "left",
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, filtered.map((s, i) => /*#__PURE__*/React.createElement("tr", {
    key: s.id,
    onClick: () => onOpen(s),
    style: {
      cursor: "pointer",
      transition: "background 120ms"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: s.customer,
    size: 28
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, s.customer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, s.id)))), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(PBadge, {
    variant: s.plan === "Enterprise" ? "plum" : s.plan === "Growth" ? "info" : "muted"
  }, s.plan)), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      textAlign: "right",
      font: "400 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, s.seats), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      textAlign: "right",
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, fmtMoney(s.mrr)), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: s.status
  })), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, s.next), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: s.owner,
    size: 22
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, s.owner.split(" ")[0]))), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      textAlign: "right",
      color: "var(--sp-text-subtle)",
      fontSize: 14,
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, "\u203A"))), filtered.length === 0 && /*#__PURE__*/React.createElement("tr", null, /*#__PURE__*/React.createElement("td", {
    colSpan: 8,
    style: {
      padding: 40,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      font: "400 13px/18px Roboto"
    }
  }, "No subscriptions match this filter."))))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center",
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "Showing ", filtered.length, " of ", SUBS.length), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "\u2039 Prev"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Next \u203A"))));
}
Object.assign(window, {
  StatusChip,
  ScreenHeader,
  EmptyState,
  LoadingRows,
  ErrorState,
  DashboardP,
  SubscriptionsP,
  KPICard
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Screens1.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Screens2.jsx
try { (() => {
// Subscriptions prototype — sub detail, invoices list, invoice detail

const {
  useState: useSt2
} = React;

// ── SUBSCRIPTION DETAIL ────────────────────────────────────────────────
function SubDetailP({
  sub,
  onBack,
  onUpgrade,
  onPause,
  onCancel,
  onEdit
}) {
  const [tab, setTab] = useSt2("overview");
  const invs = INVOICES.filter(i => i.customer === sub.customer).slice(0, 5);
  const tabs = [{
    k: "overview",
    label: "Overview"
  }, {
    k: "usage",
    label: "Usage & seats"
  }, {
    k: "invoices",
    label: "Invoices",
    count: invs.length
  }, {
    k: "addons",
    label: "Add-ons"
  }, {
    k: "history",
    label: "History"
  }];
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "24px 24px 0",
      background: "linear-gradient(180deg, var(--sp-surface) 0%, var(--sp-surface) 60%, transparent 100%)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "\u2190 Back to subscriptions")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: sub.customer,
    size: 56
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 24px/30px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, sub.customer), /*#__PURE__*/React.createElement(StatusChip, {
    s: sub.status
  }), /*#__PURE__*/React.createElement(PBadge, {
    variant: sub.plan === "Enterprise" ? "plum" : sub.plan === "Growth" ? "info" : "muted"
  }, sub.plan)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "'Roboto Mono',monospace"
    }
  }, sub.id), " \xB7 since ", sub.since, " \xB7 owner ", sub.owner)), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 28,
      lineHeight: "34px",
      color: "var(--sp-text)"
    }
  }, fmtMoney(sub.mrr), /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-subtle)",
      fontSize: 14,
      fontWeight: 400
    }
  }, " /mo")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Next charge \xB7 ", sub.next)), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      alignSelf: "flex-start",
      marginTop: 4
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u270E"
    }),
    onClick: onEdit
  }, "Edit details"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2191"
    }),
    onClick: onUpgrade
  }, "Upgrade"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      marginLeft: -4,
      marginRight: -4,
      display: "flex",
      gap: 4,
      borderBottom: "none"
    }
  }, tabs.map(t => {
    const on = tab === t.k;
    return /*#__PURE__*/React.createElement("span", {
      key: t.k,
      onClick: () => setTab(t.k),
      style: {
        padding: "10px 14px",
        cursor: "pointer",
        font: `${on ? 500 : 400} 13px/18px Roboto`,
        color: on ? "#1A73E8" : "var(--sp-text-muted)",
        borderBottom: `2px solid ${on ? "#1A73E8" : "transparent"}`,
        marginBottom: -1
      }
    }, t.label, t.count != null && /*#__PURE__*/React.createElement("span", {
      style: {
        color: "var(--sp-text-subtle)",
        marginLeft: 6,
        font: "400 12px/16px Roboto"
      }
    }, t.count));
  }))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, tab === "overview" && /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 320px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "flex-start",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      borderRadius: 12,
      background: sub.plan === "Enterprise" ? "rgba(108,92,231,.15)" : sub.plan === "Growth" ? "rgba(26,115,232,.15)" : "var(--sp-surface-2)",
      color: sub.plan === "Enterprise" ? "var(--sp-accent-plum)" : sub.plan === "Growth" ? "#1A73E8" : "var(--sp-text-muted)",
      font: "700 18px/48px Roboto",
      textAlign: "center"
    }
  }, sub.plan[0]), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, sub.plan, " plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Billed monthly \xB7 renews ", sub.next)), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: onUpgrade
  }, "Change plan")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 12,
      marginTop: 18,
      paddingTop: 16,
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(Stat, {
    label: "Seats",
    value: sub.seats
  }), /*#__PURE__*/React.createElement(Stat, {
    label: "Used",
    value: `${Math.round(sub.seats * sub.usage / 100)} / ${sub.seats}`
  }), /*#__PURE__*/React.createElement(Stat, {
    label: "Billing cycle",
    value: "Monthly"
  }), /*#__PURE__*/React.createElement(Stat, {
    label: "Auto-renew",
    value: /*#__PURE__*/React.createElement(PBadge, {
      variant: "mint",
      dot: true
    }, "On")
  }))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Seat utilization"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, sub.usage, "% of ", sub.seats, " seats in use")), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 10,
      borderRadius: 6,
      background: "var(--sp-surface-2)",
      overflow: "hidden",
      position: "relative"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: 0,
      top: 0,
      bottom: 0,
      width: `${sub.usage}%`,
      background: sub.usage > 90 ? "var(--sp-accent-warm)" : "var(--sp-accent-mint)",
      borderRadius: 6,
      transition: "width 600ms cubic-bezier(.4,0,.2,1)"
    }
  }), sub.usage > 80 && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      left: "80%",
      top: -4,
      bottom: -4,
      width: 1,
      background: "var(--sp-accent-warm)",
      opacity: .6
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      marginTop: 6,
      font: "400 10px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, /*#__PURE__*/React.createElement("span", null, "0"), /*#__PURE__*/React.createElement("span", {
    style: {
      opacity: .8
    }
  }, "\u2191 upgrade threshold (80%)"), /*#__PURE__*/React.createElement("span", null, sub.seats))), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "14px 18px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Recent invoices"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "View all \u2192")), invs.map((inv, i) => /*#__PURE__*/React.createElement("div", {
    key: inv.id,
    style: {
      padding: "12px 18px",
      display: "flex",
      alignItems: "center",
      gap: 14,
      borderBottom: i === invs.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, inv.id), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Issued ", inv.issued, " \xB7 due ", inv.due)), /*#__PURE__*/React.createElement("div", {
    className: "sp-money",
    style: {
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(inv.amount)), /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Payment method"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 36,
      height: 24,
      borderRadius: 4,
      background: "linear-gradient(135deg, #1a1f36 0%, #3d4666 100%)",
      color: "#fff",
      fontSize: 9,
      fontWeight: 700,
      textAlign: "center",
      lineHeight: "24px"
    }
  }, "VISA"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "\u2022\u2022 4242"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Expires 09/27")))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Billing contact"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: "Finance Team",
    size: 32
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "finance@", sub.customer.toLowerCase().replace(/[^a-z]/g, ""), ".com"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Accounts Payable")))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Health score"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 10,
      marginTop: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-display",
    style: {
      fontSize: 32,
      color: sub.status === "past_due" ? "var(--sp-accent-warm)" : "var(--sp-accent-mint)"
    }
  }, sub.status === "past_due" ? "42" : "86"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "/ 100")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 6
    }
  }, sub.status === "past_due" ? "Failed payment reducing score. Recover within 7 days to avoid downgrade." : "Active usage, timely payments, no flags.")), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Danger zone"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    onClick: onPause
  }, "Pause subscription"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    style: {
      color: "#D93025"
    },
    onClick: onCancel
  }, "Cancel subscription"))))), tab === "usage" && /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Seat usage over 30 days"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16,
      height: 140,
      display: "flex",
      alignItems: "flex-end",
      gap: 2
    }
  }, Array.from({
    length: 30
  }).map((_, i) => {
    const h = 30 + Math.sin(i * 0.4) * 20 + Math.random() * 40;
    return /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        flex: 1,
        height: `${h}%`,
        background: "var(--sp-accent-mint)",
        borderRadius: 3,
        opacity: 0.4 + h / 200
      }
    });
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Average ", Math.round(sub.seats * sub.usage / 100), " of ", sub.seats, " seats active daily.")), tab === "invoices" && /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    style: {
      background: "var(--sp-surface-2)"
    }
  }, ["Invoice", "Amount", "Status", "Issued", "Due", ""].map(h => /*#__PURE__*/React.createElement("th", {
    key: h,
    style: {
      padding: "10px 16px",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textAlign: "left",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, invs.map(inv => /*#__PURE__*/React.createElement("tr", {
    key: inv.id
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)",
      font: "500 13px/18px 'Roboto Mono',monospace"
    }
  }, inv.id), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)",
      font: "500 13px/18px 'Roboto Mono',monospace"
    }
  }, fmtMoney(inv.amount)), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  })), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, inv.issued), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, inv.due), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderTop: "1px solid var(--sp-border)",
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "View"))))))), tab === "addons" && /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\uFF0B",
    title: "No add-ons yet",
    body: "Stack usage-based add-ons like API calls, storage, or premium support on top of the base plan.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary"
    }, "Browse add-ons")
  }), tab === "history" && /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 0
    }
  }, [{
    at: "Oct 01, 2025",
    title: "Renewed monthly",
    body: `${sub.plan} · ${sub.seats} seats · ${fmtMoney(sub.mrr)}`,
    tone: "mint"
  }, {
    at: "Sep 14, 2025",
    title: "Seats expanded +12",
    body: `From ${sub.seats - 12} to ${sub.seats} seats, prorated`,
    tone: "plum"
  }, {
    at: "Jun 03, 2024",
    title: "Upgraded plan",
    body: `Growth → ${sub.plan}`,
    tone: "plum"
  }, {
    at: sub.since,
    title: "Subscription started",
    body: `Initial plan: ${sub.plan}`,
    tone: "info"
  }].map((h, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 14,
      padding: "14px 0",
      borderBottom: i === 3 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 80,
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, h.at), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, h.title), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, h.body))))))));
}
function Stat({
  label,
  value
}) {
  return /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginTop: 4
    }
  }, value));
}
function SideLabel({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, children);
}

// ── INVOICES LIST ──────────────────────────────────────────────────────
function InvoicesP({
  state,
  onOpen
}) {
  const [status, setStatus] = useSt2("all");
  const filtered = INVOICES.filter(i => status === "all" || i.status === status);
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Invoices"
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 8
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Invoices"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Invoices"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u20AC",
    title: "No invoices yet",
    body: "Invoices are generated when a subscription renews, or on demand.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary"
    }, "Create invoice")
  }));
  const paid = INVOICES.filter(i => i.status === "paid").reduce((a, i) => a + i.amount, 0);
  const outstanding = INVOICES.filter(i => i.status === "overdue" || i.status === "pending").reduce((a, i) => a + i.amount, 0);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Invoices",
    subtitle: "October 2025",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u21A7"
      })
    }, "Export"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      })
    }, "Create invoice"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(4, 1fr)",
      gap: 14,
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement(InvStat, {
    label: "Paid this month",
    value: fmtMoney(paid),
    tone: "mint",
    sub: `${INVOICES.filter(i => i.status === "paid").length} invoices`
  }), /*#__PURE__*/React.createElement(InvStat, {
    label: "Outstanding",
    value: fmtMoney(outstanding),
    tone: "warm",
    sub: `${INVOICES.filter(i => i.status === "overdue").length} overdue`
  }), /*#__PURE__*/React.createElement(InvStat, {
    label: "Pending settlement",
    value: fmtMoney(INVOICES.filter(i => i.status === "pending").reduce((a, i) => a + i.amount, 0)),
    tone: "amber",
    sub: "1 invoice"
  }), /*#__PURE__*/React.createElement(InvStat, {
    label: "Drafts",
    value: INVOICES.filter(i => i.status === "draft").length,
    tone: "muted",
    sub: "Not yet sent"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    value: status,
    onChange: setStatus,
    size: "sm",
    options: [{
      value: "all",
      label: `All (${INVOICES.length})`
    }, {
      value: "paid",
      label: "Paid"
    }, {
      value: "pending",
      label: "Pending"
    }, {
      value: "overdue",
      label: "Overdue"
    }, {
      value: "draft",
      label: "Draft"
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    compact: true,
    placeholder: "Search invoice #\u2026",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2315"
    }),
    style: {
      width: 240
    }
  })), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", {
    style: {
      background: "var(--sp-surface-2)"
    }
  }, ["Invoice", "Customer", "Issued", "Due", "Amount", "Status", ""].map((h, i) => /*#__PURE__*/React.createElement("th", {
    key: i,
    style: {
      padding: "12px 16px",
      textAlign: i === 4 ? "right" : "left",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, filtered.map((inv, i) => /*#__PURE__*/React.createElement("tr", {
    key: inv.id,
    onClick: () => onOpen(inv),
    style: {
      cursor: "pointer"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, inv.id), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: inv.customer,
    size: 24
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, inv.customer))), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, inv.issued), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      font: "400 13px/18px Roboto",
      color: inv.status === "overdue" ? "var(--sp-accent-warm)" : "var(--sp-text-muted)",
      fontWeight: inv.status === "overdue" ? 500 : 400
    }
  }, inv.due), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      textAlign: "right",
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, inv.amount ? fmtMoney(inv.amount) : "—"), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  })), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 16px",
      borderBottom: i === filtered.length - 1 ? "none" : "1px solid var(--sp-border)",
      textAlign: "right",
      color: "var(--sp-text-subtle)"
    }
  }, "\u203A")))))));
}
function InvStat({
  label,
  value,
  tone,
  sub
}) {
  const colors = {
    mint: "var(--sp-accent-mint)",
    warm: "var(--sp-accent-warm)",
    amber: "#B06000",
    muted: "var(--sp-text-muted)"
  }[tone];
  return /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, label), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 22,
      lineHeight: "28px",
      marginTop: 6,
      color: colors || "var(--sp-text)"
    }
  }, value), sub && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, sub));
}

// ── INVOICE DETAIL ─────────────────────────────────────────────────────
function InvoiceDetailP({
  inv,
  onBack,
  onCollect
}) {
  const items = [{
    name: `${inv.customer === "Acme Holdings" ? "Enterprise" : inv.customer === "Orbit Labs" ? "Growth" : "Growth"} plan — Monthly`,
    qty: 1,
    unit: inv.amount,
    total: inv.amount
  }];
  const subtotal = items.reduce((a, i) => a + i.total, 0);
  const tax = Math.round(subtotal * 0.21);
  const total = subtotal + tax;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "\u2190 Back to invoices")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 340px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, {
    pad: 0,
    style: {
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "28px 32px",
      borderBottom: "2px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "flex-start"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 32,
      height: 32,
      borderRadius: 8,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff",
      fontWeight: 700,
      fontSize: 14
    }
  }, "iC"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 15px/20px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.02em"
    }
  }, "Incedo / CRM")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 14
    }
  }, "Incedo B.V. \xB7 Hoogoorddreef 9 \xB7 1101 BA Amsterdam", /*#__PURE__*/React.createElement("br", null), "VAT NL856123456B01 \xB7 billing@incedo.nl")), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.08em"
    }
  }, "Invoice"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 17px/22px 'Roboto Mono',monospace",
      color: "var(--sp-text)",
      marginTop: 2
    }
  }, inv.id), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(StatusChip, {
    s: inv.status
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "20px 32px",
      borderBottom: "1px solid var(--sp-border)",
      display: "grid",
      gridTemplateColumns: "1fr 1fr 1fr",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Bill to"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, inv.customer), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Attn: Accounts Payable", /*#__PURE__*/React.createElement("br", null), "finance@", inv.customer.toLowerCase().replace(/[^a-z]/g, ""), ".com")), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Issued"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      marginTop: 6
    }
  }, inv.issued, ", 2025"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      marginTop: 10
    }
  }, "Due"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: inv.status === "overdue" ? "var(--sp-accent-warm)" : "var(--sp-text)",
      marginTop: 2
    }
  }, inv.due, ", 2025")), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Amount due"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 28,
      lineHeight: "34px",
      marginTop: 6,
      color: "var(--sp-text)"
    }
  }, fmtMoney(total)))), /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", null, ["Description", "Qty", "Unit price", "Amount"].map((h, i) => /*#__PURE__*/React.createElement("th", {
    key: i,
    style: {
      padding: "14px 32px",
      textAlign: i === 0 ? "left" : "right",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, h)))), /*#__PURE__*/React.createElement("tbody", null, items.map((it, i) => /*#__PURE__*/React.createElement("tr", {
    key: i
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "18px 32px",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, it.name, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "Billing period: Oct 01 \u2013 Oct 31, 2025")), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "18px 32px",
      textAlign: "right",
      font: "400 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, it.qty), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "18px 32px",
      textAlign: "right",
      font: "400 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(it.unit)), /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "18px 32px",
      textAlign: "right",
      font: "500 13px/18px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtMoney(it.total)))))), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 32px 28px",
      display: "flex",
      justifyContent: "flex-end"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 260
    }
  }, /*#__PURE__*/React.createElement(SummaryRow, {
    label: "Subtotal",
    value: fmtMoney(subtotal)
  }), /*#__PURE__*/React.createElement(SummaryRow, {
    label: "VAT (21%)",
    value: fmtMoney(tax)
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "2px solid var(--sp-border)",
      marginTop: 8,
      paddingTop: 8
    }
  }, /*#__PURE__*/React.createElement(SummaryRow, {
    bold: true,
    label: "Total",
    value: fmtMoney(total)
  }))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, inv.status === "overdue" && /*#__PURE__*/React.createElement(PCard, {
    style: {
      borderLeft: "3px solid var(--sp-accent-warm)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-accent-warm)"
    }
  }, "This invoice is overdue"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4
    }
  }, inv.attempts || 2, " failed collection attempts. Next automatic retry in 2 days."), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "warm",
    size: "sm",
    onClick: onCollect
  }, "Retry now"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Contact customer"))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Actions"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 6,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    })
  }, "Send to customer"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A7"
    })
  }, "Download PDF"), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2398"
    })
  }, "Copy payment link"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u21A9"
    })
  }, "Issue refund"))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Payment timeline"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 10,
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(TL, {
    at: "Oct 01",
    title: "Invoice created",
    tone: "info",
    icon: "\u270E"
  }), /*#__PURE__*/React.createElement(TL, {
    at: "Oct 01",
    title: "Sent to customer",
    tone: "info",
    icon: "\u2709"
  }), inv.status === "overdue" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(TL, {
    at: "Oct 15",
    title: "Auto-collection attempt",
    body: "Failed \xB7 insufficient_funds",
    tone: "warm",
    icon: "\u2715"
  }), /*#__PURE__*/React.createElement(TL, {
    at: "Oct 17",
    title: "Retry attempt",
    body: "Failed \xB7 insufficient_funds",
    tone: "warm",
    icon: "\u2715"
  }), /*#__PURE__*/React.createElement(TL, {
    at: "Oct 20",
    title: "Next retry scheduled",
    tone: "muted",
    icon: "\u23F1"
  })), inv.status === "paid" && /*#__PURE__*/React.createElement(TL, {
    at: inv.due,
    title: "Payment received",
    body: "Visa \u2022\u2022 4242",
    tone: "mint",
    icon: "\u2713"
  }))))));
}
function SummaryRow({
  label,
  value,
  bold
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "4px 0",
      font: `${bold ? 600 : 400} 13px/18px Roboto`,
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: bold ? "var(--sp-text)" : "var(--sp-text-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      fontFamily: "'Roboto Mono',monospace",
      fontWeight: bold ? 700 : 500
    }
  }, value));
}
function TL({
  at,
  title,
  body,
  tone,
  icon
}) {
  const colors = {
    mint: ["var(--sp-accent-mint)", "rgba(0,184,148,.14)"],
    warm: ["var(--sp-accent-warm)", "rgba(255,122,89,.14)"],
    info: ["#1A73E8", "rgba(26,115,232,.12)"],
    muted: ["var(--sp-text-muted)", "var(--sp-surface-2)"]
  }[tone];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 22,
      height: 22,
      borderRadius: "50%",
      flexShrink: 0,
      background: colors[1],
      color: colors[0],
      font: "600 11px/22px Roboto",
      textAlign: "center"
    }
  }, icon), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, title), body && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 1
    }
  }, body)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, at));
}
Object.assign(window, {
  SubDetailP,
  InvoicesP,
  InvoiceDetailP
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Screens2.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Screens3.jsx
try { (() => {
// Subscriptions prototype — dunning, plans, payment methods, checkout

const {
  useState: useSt3
} = React;

// ── DUNNING QUEUE ──────────────────────────────────────────────────────
function DunningP({
  state,
  onOpenInvoice
}) {
  const [sev, setSev] = useSt3("all");
  const filtered = DUNNING.filter(d => sev === "all" || d.severity === sev);
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Dunning queue"
  }), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement(LoadingRows, {
    count: 5
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Dunning queue"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Dunning queue"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u2713",
    title: "All clear \u2014 no failed payments",
    body: "Failed payments that need attention will appear here. Automatic retries continue in the background."
  }));
  const atRisk = DUNNING.reduce((a, d) => a + d.amount, 0);
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Dunning queue",
    subtitle: "Failed payments requiring action or automated recovery",
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary"
    }, "Pause automations"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u26A1"
      })
    }, "Run recovery now"))
  }), /*#__PURE__*/React.createElement(PCard, {
    style: {
      background: "linear-gradient(135deg, rgba(255,122,89,.08) 0%, rgba(244,180,0,.05) 100%)",
      marginBottom: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 24
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.08em"
    }
  }, "Revenue at risk"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 36,
      lineHeight: "44px",
      color: "var(--sp-accent-warm)",
      marginTop: 4
    }
  }, fmtMoney(atRisk)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 2
    }
  }, "across ", DUNNING.length, " invoices \xB7 auto-recovery rate 74%")), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      gap: 6,
      alignItems: "flex-end",
      height: 80
    }
  }, DUNNING.map((d, i) => /*#__PURE__*/React.createElement("div", {
    key: d.id,
    style: {
      flex: 1,
      display: "flex",
      flexDirection: "column",
      alignItems: "center",
      gap: 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: "100%",
      height: `${Math.min(100, d.amount / 1000)}%`,
      background: d.severity === "high" ? "var(--sp-accent-warm)" : d.severity === "medium" ? "#F4B400" : "var(--sp-accent-mint)",
      borderRadius: 4,
      opacity: 0.85
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 9px/12px 'Roboto Mono',monospace",
      color: "var(--sp-text-subtle)"
    }
  }, fmtShortMoney(d.amount))))))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 10,
      alignItems: "center",
      marginBottom: 14
    }
  }, /*#__PURE__*/React.createElement(PSegmented, {
    value: sev,
    onChange: setSev,
    size: "sm",
    options: [{
      value: "all",
      label: `All (${DUNNING.length})`
    }, {
      value: "high",
      label: `High (${DUNNING.filter(d => d.severity === "high").length})`
    }, {
      value: "medium",
      label: "Medium"
    }, {
      value: "low",
      label: "Low"
    }]
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, filtered.map(d => {
    const sevColor = d.severity === "high" ? "var(--sp-accent-warm)" : d.severity === "medium" ? "#F4B400" : "var(--sp-accent-mint)";
    const reasonLabel = {
      insufficient_funds: "Insufficient funds",
      expired_card: "Expired card",
      do_not_honor: "Bank declined (do_not_honor)",
      authentication: "3DS authentication failed"
    }[d.reason];
    return /*#__PURE__*/React.createElement(PCard, {
      key: d.id,
      hover: true,
      style: {
        borderLeft: `3px solid ${sevColor}`
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 16
      }
    }, /*#__PURE__*/React.createElement(PAvatar, {
      name: d.customer,
      size: 40
    }), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        minWidth: 0
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "600 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, d.customer), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px 'Roboto Mono',monospace",
        color: "var(--sp-text-subtle)"
      }
    }, "\xB7 ", d.id)), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 10,
        alignItems: "center",
        marginTop: 4
      }
    }, /*#__PURE__*/React.createElement(PBadge, {
      variant: d.severity === "high" ? "warm" : d.severity === "medium" ? "amber" : "mint",
      dot: true
    }, d.severity), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, reasonLabel), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 12px/16px Roboto",
        color: "var(--sp-text-subtle)"
      }
    }, "\xB7 ", d.age, " days overdue"))), /*#__PURE__*/React.createElement("div", {
      style: {
        textAlign: "right",
        minWidth: 120
      }
    }, /*#__PURE__*/React.createElement("div", {
      className: "sp-money",
      style: {
        font: "600 16px/22px 'Roboto Mono',monospace",
        color: "var(--sp-text)"
      }
    }, fmtMoney(d.amount)), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "Next retry ", d.nextRetry)), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 6
      }
    }, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      size: "sm"
    }, "Message"), /*#__PURE__*/React.createElement(PButton, {
      variant: "warm",
      size: "sm"
    }, "Collect now"))), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 4,
        marginTop: 14,
        alignItems: "center"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 10px/14px Roboto",
        color: "var(--sp-text-muted)",
        width: 70
      }
    }, "Attempts"), Array.from({
      length: 5
    }).map((_, i) => /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        width: 28,
        height: 4,
        borderRadius: 2,
        background: i < d.attempts ? sevColor : "var(--sp-surface-2)"
      }
    })), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        marginLeft: 6
      }
    }, d.attempts, " / 5"), /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1
      }
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, "Owner: ", d.owner)));
  })));
}

// ── PLANS & PRICING ────────────────────────────────────────────────────
function PlansP({
  state,
  onEdit
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Plans & pricing"
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3,1fr)",
      gap: 16
    }
  }, [0, 1, 2].map(i => /*#__PURE__*/React.createElement(PCard, {
    key: i
  }, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "50%",
    h: 18
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 16
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "80%",
    h: 40
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 80
  })))));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Plans & pricing"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Plans & pricing"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25C7",
    title: "No plans defined",
    body: "Create tiered plans your sales team can quote. Each plan is a reusable bundle of price, features, and limits.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary"
    }, "Create first plan")
  }));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Plans & pricing",
    subtitle: `${PLANS.length} live plans · ${PLANS.reduce((a, p) => a + p.subs, 0)} active subscriptions`,
    actions: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(PButton, {
      variant: "secondary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "\u29EB"
      })
    }, "Archive"), /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      })
    }, "New plan"))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3, 1fr)",
      gap: 16,
      marginBottom: 24
    }
  }, PLANS.map(p => /*#__PURE__*/React.createElement(PCard, {
    key: p.id,
    hover: true,
    style: {
      position: "relative",
      overflow: "hidden",
      borderTop: `3px solid ${p.color}`
    }
  }, p.id === "growth" && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "absolute",
      top: 12,
      right: 12
    }
  }, /*#__PURE__*/React.createElement(PBadge, {
    variant: "mint",
    dot: true
  }, "Most popular")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, p.name), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "baseline",
      gap: 4,
      marginTop: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "sp-display",
    style: {
      fontSize: 34,
      color: "var(--sp-text)"
    }
  }, fmtMoney(p.price)), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "/ ", p.interval)), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, p.seats ? `up to ${p.seats} seats` : "unlimited seats"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16,
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, p.feats.map((f, i) => /*#__PURE__*/React.createElement("div", {
    key: i,
    style: {
      display: "flex",
      gap: 8,
      font: "400 12px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: p.color
    }
  }, "\u2713"), " ", f))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      paddingTop: 14,
      borderTop: "1px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 10px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "Active subs"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, p.subs)), /*#__PURE__*/React.createElement("div", {
    style: {
      textAlign: "right"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 10px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em"
    }
  }, "MRR"), /*#__PURE__*/React.createElement("div", {
    className: "sp-money",
    style: {
      font: "600 15px/20px 'Roboto Mono',monospace",
      color: "var(--sp-text)"
    }
  }, fmtShortMoney(p.mrr))), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: () => onEdit?.(p)
  }, "Edit"))))), /*#__PURE__*/React.createElement(PCard, {
    pad: 0
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "14px 20px",
      borderBottom: "1px solid var(--sp-border)",
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Feature comparison"), /*#__PURE__*/React.createElement("table", {
    style: {
      width: "100%",
      borderCollapse: "collapse"
    }
  }, /*#__PURE__*/React.createElement("thead", null, /*#__PURE__*/React.createElement("tr", null, /*#__PURE__*/React.createElement("th", {
    style: {
      padding: "12px 20px",
      textAlign: "left",
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: "0.06em",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, "Feature"), PLANS.map(p => /*#__PURE__*/React.createElement("th", {
    key: p.id,
    style: {
      padding: "12px 20px",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, p.name)))), /*#__PURE__*/React.createElement("tbody", null, [["Pipelines", "1", "Unlimited", "Unlimited"], ["API access", "—", "✓", "✓"], ["Automations/month", "50", "2,000", "Unlimited"], ["SSO / SAML", "—", "—", "✓"], ["Dedicated CSM", "—", "—", "✓"], ["Data residency", "EU", "EU", "EU / US / custom"], ["Support SLA", "48h", "Same business day", "1h · 99.95%"]].map((row, i) => /*#__PURE__*/React.createElement("tr", {
    key: i
  }, /*#__PURE__*/React.createElement("td", {
    style: {
      padding: "12px 20px",
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, row[0]), row.slice(1).map((v, j) => /*#__PURE__*/React.createElement("td", {
    key: j,
    style: {
      padding: "12px 20px",
      textAlign: "center",
      font: "400 13px/18px Roboto",
      color: v === "—" ? "var(--sp-text-subtle)" : "var(--sp-text)",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, v))))))));
}

// ── PAYMENT METHODS / BILLING SETTINGS ─────────────────────────────────
function PaymentsP({
  state,
  onAdd
}) {
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Payment methods"
  }), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "60%",
    h: 18
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 60
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Payment methods"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Payment methods"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u25A3",
    title: "No payment methods on file",
    body: "Add a card or SEPA mandate to enable automatic collection on subscriptions.",
    cta: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      onClick: onAdd
    }, "Add payment method")
  }));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Billing & payment methods",
    subtitle: "Manage how you accept and disburse payments",
    actions: /*#__PURE__*/React.createElement(PButton, {
      variant: "primary",
      leading: /*#__PURE__*/React.createElement(Ico, {
        g: "+"
      }),
      onClick: onAdd
    }, "Add method")
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 360px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, PAYMENT_METHODS.map(pm => /*#__PURE__*/React.createElement(PCard, {
    key: pm.id,
    hover: true
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 56,
      height: 36,
      borderRadius: 6,
      flexShrink: 0,
      background: pm.brand === "Visa" ? "linear-gradient(135deg, #1a1f36 0%, #3d4666 100%)" : pm.brand === "Mastercard" ? "linear-gradient(135deg, #EB001B 0%, #F79E1B 100%)" : "linear-gradient(135deg, var(--sp-accent-plum) 0%, #1A73E8 100%)",
      color: "#fff",
      fontSize: 11,
      fontWeight: 700,
      textAlign: "center",
      lineHeight: "36px"
    }
  }, pm.brand), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "\u2022\u2022\u2022\u2022 ", pm.last4), pm.default && /*#__PURE__*/React.createElement(PBadge, {
    variant: "info",
    dot: true
  }, "Default")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, pm.exp ? `Expires ${pm.exp}` : "SEPA Direct Debit mandate", " \xB7 Added ", pm.added)), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Manage")))), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 12
    }
  }, "Billing details"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement(PInput, {
    label: "Legal entity",
    value: "Incedo B.V."
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "VAT number",
    value: "NL856123456B01"
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Billing email",
    value: "billing@incedo.nl",
    style: {
      gridColumn: "span 2"
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Address line 1",
    value: "Hoogoorddreef 9",
    style: {
      gridColumn: "span 2"
    }
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "City",
    value: "Amsterdam"
  }), /*#__PURE__*/React.createElement(PInput, {
    label: "Postal code",
    value: "1101 BA"
  })))), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 14
    }
  }, /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(SideLabel, null, "Collection preferences"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 12,
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(Pref, {
    label: "Retry failed charges",
    value: "After 3 / 5 / 7 days"
  }), /*#__PURE__*/React.createElement(Pref, {
    label: "Max retry attempts",
    value: "5"
  }), /*#__PURE__*/React.createElement(Pref, {
    label: "Dunning emails",
    value: "On (3 templates)"
  }), /*#__PURE__*/React.createElement(Pref, {
    label: "Auto-suspend after",
    value: "21 days overdue"
  }))), /*#__PURE__*/React.createElement(PCard, {
    style: {
      background: "linear-gradient(135deg, rgba(0,184,148,.08), transparent)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 32,
      height: 32,
      borderRadius: 8,
      background: "rgba(0,184,148,.14)",
      color: "var(--sp-accent-mint)",
      font: "700 14px/32px Roboto",
      textAlign: "center"
    }
  }, "\u20AC"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, "Stripe connected")), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 8
    }
  }, "Webhooks healthy \xB7 last sync 2 min ago \xB7 acct_1NxK2L"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 6,
      marginTop: 12
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm"
  }, "Open dashboard"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm"
  }, "Disconnect"))))));
}
function Pref({
  label,
  value
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      justifyContent: "space-between",
      padding: "6px 0",
      borderBottom: "1px solid var(--sp-border)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, label), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)"
    }
  }, value));
}

// ── CHECKOUT / UPGRADE FLOW ────────────────────────────────────────────
function CheckoutP({
  state,
  onBack,
  onConfirm
}) {
  const [step, setStep] = useSt3(1);
  const [plan, setPlan] = useSt3("growth");
  const [seats, setSeats] = useSt3(42);
  const [interval, setInterval] = useSt3("mo");
  const p = PLANS.find(x => x.id === plan);
  const unit = interval === "yr" ? Math.round(p.price * 10) : p.price; // annual = 10x (2mo free)
  const subtotal = unit * (p.id === "enterprise" ? 1 : seats / (p.seats || 1));
  const total = Math.round(subtotal);
  if (state === "loading") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Upgrade subscription"
  }), /*#__PURE__*/React.createElement(PCard, null, /*#__PURE__*/React.createElement(PSkeleton, {
    w: "100%",
    h: 200
  })));
  if (state === "error") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Upgrade subscription"
  }), /*#__PURE__*/React.createElement(ErrorState, null));
  if (state === "empty") return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement(ScreenHeader, {
    title: "Upgrade subscription"
  }), /*#__PURE__*/React.createElement(EmptyState, {
    icon: "\u2191",
    title: "Nothing to upgrade from",
    body: "Select a subscription first."
  }));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 24
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      marginBottom: 16
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onBack,
    style: {
      cursor: "pointer",
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "\u2190 Cancel upgrade")), /*#__PURE__*/React.createElement("h1", {
    style: {
      font: "700 28px/34px Roboto",
      color: "var(--sp-text)",
      margin: 0,
      letterSpacing: "-0.02em"
    }
  }, "Upgrade Orbit Labs"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 4,
      marginBottom: 24
    }
  }, "Current plan: ", /*#__PURE__*/React.createElement("b", {
    style: {
      color: "var(--sp-text)"
    }
  }, "Growth \xB7 42 seats \xB7 ", fmtMoney(21600), "/mo")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 8,
      marginBottom: 24
    }
  }, ["Plan", "Seats & interval", "Payment", "Review"].map((s, i) => {
    const n = i + 1;
    const on = n === step;
    const done = n < step;
    return /*#__PURE__*/React.createElement("div", {
      key: s,
      style: {
        flex: 1,
        display: "flex",
        alignItems: "center",
        gap: 10
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        width: 28,
        height: 28,
        borderRadius: "50%",
        background: done ? "var(--sp-accent-mint)" : on ? "#1A73E8" : "var(--sp-surface-2)",
        color: done || on ? "#fff" : "var(--sp-text-muted)",
        font: "600 12px/28px Roboto",
        textAlign: "center",
        flexShrink: 0
      }
    }, done ? "✓" : n), /*#__PURE__*/React.createElement("span", {
      style: {
        font: `${on ? 600 : 400} 13px/18px Roboto`,
        color: on ? "var(--sp-text)" : "var(--sp-text-muted)"
      }
    }, s), i < 3 && /*#__PURE__*/React.createElement("div", {
      style: {
        flex: 1,
        height: 2,
        background: done ? "var(--sp-accent-mint)" : "var(--sp-border)",
        borderRadius: 1
      }
    }));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 360px",
      gap: 20
    }
  }, /*#__PURE__*/React.createElement(PCard, null, step === 1 && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "Choose a plan"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "repeat(3,1fr)",
      gap: 10
    }
  }, PLANS.map(pl => {
    const on = plan === pl.id;
    return /*#__PURE__*/React.createElement("div", {
      key: pl.id,
      onClick: () => setPlan(pl.id),
      style: {
        padding: 16,
        borderRadius: 12,
        cursor: "pointer",
        border: `2px solid ${on ? pl.color : "var(--sp-border)"}`,
        background: on ? `${pl.color}10` : "transparent",
        transition: "all 150ms"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between"
      }
    }, /*#__PURE__*/React.createElement("span", {
      style: {
        font: "600 14px/20px Roboto",
        color: "var(--sp-text)"
      }
    }, pl.name), /*#__PURE__*/React.createElement("span", {
      style: {
        width: 18,
        height: 18,
        borderRadius: "50%",
        border: `2px solid ${on ? pl.color : "var(--sp-border)"}`,
        background: on ? pl.color : "transparent"
      }
    })), /*#__PURE__*/React.createElement("div", {
      className: "sp-money",
      style: {
        font: "700 20px/26px 'Roboto Mono',monospace",
        color: "var(--sp-text)",
        marginTop: 10
      }
    }, fmtMoney(pl.price), /*#__PURE__*/React.createElement("span", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, " /mo")), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "400 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        marginTop: 2
      }
    }, pl.seats ? `up to ${pl.seats} seats` : "unlimited seats"));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24,
      display: "flex",
      justifyContent: "flex-end"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: () => setStep(2)
  }, "Continue \u2192"))), step === 2 && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "Seats and billing interval"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      marginBottom: 24
    }
  }, /*#__PURE__*/React.createElement("label", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text-muted)",
      flex: 1
    }
  }, "Seats", /*#__PURE__*/React.createElement("input", {
    type: "range",
    min: 5,
    max: p.seats || 300,
    value: seats,
    onChange: e => setSeats(Number(e.target.value)),
    style: {
      width: "100%",
      marginTop: 8,
      accentColor: "#1A73E8"
    }
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      minWidth: 80,
      textAlign: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "sp-money sp-display",
    style: {
      fontSize: 36,
      color: "var(--sp-text)"
    }
  }, seats), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "seats"))), /*#__PURE__*/React.createElement(PSegmented, {
    value: interval,
    onChange: setInterval,
    options: [{
      value: "mo",
      label: "Monthly"
    }, {
      value: "yr",
      label: "Annual · Save 16%"
    }]
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24,
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    onClick: () => setStep(1)
  }, "\u2190 Back"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: () => setStep(3)
  }, "Continue \u2192"))), step === 3 && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "Payment method"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexDirection: "column",
      gap: 8
    }
  }, PAYMENT_METHODS.map((pm, i) => /*#__PURE__*/React.createElement("div", {
    key: pm.id,
    style: {
      padding: 12,
      borderRadius: 10,
      border: `2px solid ${i === 0 ? "#1A73E8" : "var(--sp-border)"}`,
      display: "flex",
      alignItems: "center",
      gap: 12,
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 18,
      height: 18,
      borderRadius: "50%",
      border: `2px solid ${i === 0 ? "#1A73E8" : "var(--sp-border)"}`,
      background: i === 0 ? "#1A73E8" : "transparent"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, pm.brand, " \u2022\u2022\u2022\u2022 ", pm.last4), pm.default && /*#__PURE__*/React.createElement(PBadge, {
    variant: "info"
  }, "Default"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, pm.exp || "SEPA"))), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "+"
    })
  }, "Add new method")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24,
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    onClick: () => setStep(2)
  }, "\u2190 Back"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    onClick: () => setStep(4)
  }, "Review order \u2192"))), step === 4 && /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)",
      marginBottom: 14
    }
  }, "Review changes"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      padding: 16,
      borderRadius: 10,
      background: "var(--sp-surface-2)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "From"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "Growth \xB7 42 seats \xB7 Monthly")), /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 22,
      color: "var(--sp-accent-plum)"
    }
  }, "\u2192"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "To"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, p.name, " \xB7 ", seats, " seats \xB7 ", interval === "yr" ? "Annual" : "Monthly"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 20,
      padding: "12px 14px",
      borderRadius: 10,
      background: "rgba(0,184,148,.08)",
      font: "400 12px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, "\u2713 Prorated credit of ", /*#__PURE__*/React.createElement("b", null, "\u20AC12,450.00"), " will be applied for unused days on the current plan."), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 24,
      display: "flex",
      justifyContent: "space-between"
    }
  }, /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    onClick: () => setStep(3)
  }, "\u2190 Back"), /*#__PURE__*/React.createElement(PButton, {
    variant: "mint",
    size: "lg",
    onClick: onConfirm
  }, "Confirm upgrade \xB7 ", fmtMoney(total))))), /*#__PURE__*/React.createElement(PCard, {
    style: {
      position: "sticky",
      top: 0,
      alignSelf: "flex-start"
    }
  }, /*#__PURE__*/React.createElement(SideLabel, null, "Order summary"), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 14,
      display: "flex",
      flexDirection: "column",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement(SummaryRow, {
    label: `${p.name} plan`,
    value: fmtMoney(unit)
  }), p.id !== "enterprise" && /*#__PURE__*/React.createElement(SummaryRow, {
    label: `${seats} seats × ${fmtMoney(unit / (p.seats || 1))}`,
    value: fmtMoney(total)
  }), /*#__PURE__*/React.createElement(SummaryRow, {
    label: "Prorated credit",
    value: `−${fmtMoney(1245000)}`
  }), /*#__PURE__*/React.createElement(SummaryRow, {
    label: "VAT (21%)",
    value: fmtMoney(Math.round(total * 0.21))
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "2px solid var(--sp-border)",
      marginTop: 14,
      paddingTop: 12
    }
  }, /*#__PURE__*/React.createElement(SummaryRow, {
    bold: true,
    label: "Due today",
    value: fmtMoney(Math.max(0, total + Math.round(total * 0.21) - 1245000))
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginTop: 8
    }
  }, "Charged to Visa \u2022\u2022\u2022\u2022 4242 \xB7 ", interval === "yr" ? "annually" : "monthly", ", auto-renews.")))));
}
Object.assign(window, {
  DunningP,
  PlansP,
  PaymentsP,
  CheckoutP
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Screens3.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/sub_Shell.jsx
try { (() => {
// Subscriptions prototype — shell (sidenav + topbar + canvas)

const {
  useState: useStateSh,
  useEffect: useEffectSh
} = React;
const NAV = [{
  route: "dashboard",
  label: "Dashboard",
  icon: "⌂",
  group: "Work"
}, {
  route: "my-work",
  label: "My work",
  icon: "☑",
  group: "Work",
  badge: "8",
  highlight: true
}, {
  route: "contacts",
  label: "Contacts",
  icon: "☰",
  group: "Work",
  badge: "1,284"
}, {
  route: "companies",
  label: "Companies",
  icon: "⎈",
  group: "Work",
  badge: "312"
}, {
  route: "deals",
  label: "Deals",
  icon: "♦",
  group: "Work",
  badge: "14"
}, {
  route: "activities",
  label: "Activities",
  icon: "⏱",
  group: "Work"
}, {
  sep: true,
  label: "Revenue"
}, {
  route: "subscriptions",
  label: "Subscriptions",
  icon: "⟳",
  group: "Revenue",
  badge: "248",
  highlight: true
}, {
  route: "invoices",
  label: "Invoices",
  icon: "€",
  group: "Revenue"
}, {
  route: "dunning",
  label: "Dunning",
  icon: "⚠",
  group: "Revenue",
  badge: "17"
}, {
  route: "plans",
  label: "Plans & pricing",
  icon: "◇",
  group: "Revenue"
}, {
  route: "pricing-catalog",
  label: "Pricing catalog",
  icon: "▤",
  group: "Revenue"
}, {
  route: "payments",
  label: "Payment methods",
  icon: "▣",
  group: "Revenue"
}, {
  route: "payments-list",
  label: "Payments",
  icon: "€",
  group: "Revenue"
}, {
  route: "sepa",
  label: "SEPA",
  icon: "⇄",
  group: "Revenue"
}, {
  route: "ledger",
  label: "Ledger",
  icon: "≡",
  group: "Revenue"
}, {
  route: "templates",
  label: "Templates",
  icon: "✉",
  group: "Revenue"
}, {
  sep: true,
  label: "Customer Care"
}, {
  route: "complaints",
  label: "Complaints",
  icon: "◉",
  group: "Customer Care",
  badge: "7",
  highlight: true
}, {
  route: "tickets",
  label: "Support tickets",
  icon: "✉",
  group: "Customer Care",
  badge: "12"
}, {
  route: "chat",
  label: "Chat",
  icon: "◧",
  group: "Customer Care",
  badge: "6",
  highlight: true
}, {
  route: "channels",
  label: "Channels",
  icon: "✆",
  group: "Customer Care"
}, {
  sep: true,
  label: "System"
}, {
  route: "reports",
  label: "Reports",
  icon: "◨",
  group: "System"
}, {
  route: "settings",
  label: "Settings",
  icon: "✦",
  group: "System"
}, {
  route: "customer-edit",
  label: "Customer details",
  icon: "ⓘ",
  group: "System"
}];
function SideNavP({
  current,
  onNavigate,
  collapsed,
  onToggle,
  theme,
  onTheme
}) {
  const w = collapsed ? 68 : 236;
  return /*#__PURE__*/React.createElement("aside", {
    style: {
      width: w,
      background: "var(--sp-surface)",
      padding: "14px 10px",
      display: "flex",
      flexDirection: "column",
      borderRight: "1px solid var(--sp-border)",
      flexShrink: 0,
      transition: "width 200ms cubic-bezier(.4,0,.2,1)",
      overflow: "hidden"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "6px 10px 16px"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 28,
      height: 28,
      borderRadius: 8,
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center",
      flexShrink: 0,
      background: "linear-gradient(135deg, #1A73E8 0%, var(--sp-accent-plum) 100%)",
      color: "#fff",
      fontWeight: 700,
      fontSize: 13,
      letterSpacing: "-0.04em"
    }
  }, "iC"), !collapsed && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "700 16px/20px Roboto",
      color: "var(--sp-text)",
      letterSpacing: "-0.02em"
    }
  }, "Incedo", /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontWeight: 400
    }
  }, " / CRM"))), /*#__PURE__*/React.createElement("div", {
    className: "sp-scroll",
    style: {
      flex: 1,
      overflow: "auto",
      margin: "0 -4px",
      padding: "0 4px"
    }
  }, NAV.map((it, i) => {
    if (it.sep) return !collapsed ? /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        font: "500 10px/14px Roboto",
        color: "var(--sp-text-subtle)",
        textTransform: "uppercase",
        letterSpacing: "0.08em",
        padding: "14px 12px 6px"
      }
    }, it.label) : /*#__PURE__*/React.createElement("div", {
      key: i,
      style: {
        height: 12
      }
    });
    const on = it.route === current;
    return /*#__PURE__*/React.createElement("div", {
      key: it.route,
      onClick: () => onNavigate(it.route),
      style: {
        display: "flex",
        alignItems: "center",
        gap: 12,
        padding: collapsed ? "10px" : "9px 12px",
        justifyContent: collapsed ? "center" : "flex-start",
        borderRadius: 8,
        cursor: "pointer",
        marginBottom: 2,
        background: on ? "linear-gradient(90deg, rgba(26,115,232,.14), rgba(108,92,231,.08))" : "transparent",
        color: on ? "#1A73E8" : "var(--sp-text-muted)",
        fontWeight: on ? 500 : 400,
        font: `${on ? 500 : 400} 13px/18px Roboto`,
        position: "relative",
        transition: "background 120ms"
      },
      onMouseEnter: e => {
        if (!on) e.currentTarget.style.background = "var(--sp-surface-2)";
      },
      onMouseLeave: e => {
        if (!on) e.currentTarget.style.background = "transparent";
      }
    }, on && /*#__PURE__*/React.createElement("span", {
      style: {
        position: "absolute",
        left: -10,
        top: 6,
        bottom: 6,
        width: 3,
        background: "#1A73E8",
        borderRadius: 2
      }
    }), /*#__PURE__*/React.createElement("span", {
      style: {
        width: 18,
        textAlign: "center",
        fontSize: 14,
        opacity: on ? 1 : 0.85
      }
    }, it.icon), !collapsed && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("span", {
      style: {
        flex: 1,
        whiteSpace: "nowrap",
        overflow: "hidden",
        textOverflow: "ellipsis"
      }
    }, it.label), it.badge && /*#__PURE__*/React.createElement("span", {
      style: {
        font: "500 10px/14px Roboto",
        color: it.highlight ? "var(--sp-accent-mint)" : "var(--sp-text-subtle)",
        background: it.highlight ? "rgba(0,184,148,.14)" : "var(--sp-surface-2)",
        padding: "1px 7px",
        borderRadius: 10
      }
    }, it.badge)));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      borderTop: "1px solid var(--sp-border)",
      paddingTop: 10,
      marginTop: 6,
      display: "flex",
      flexDirection: collapsed ? "column" : "row",
      gap: 6,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement(PAvatar, {
    name: "Anna Krause",
    size: 28
  }), !collapsed && /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 12px/16px Roboto",
      color: "var(--sp-text)",
      whiteSpace: "nowrap",
      overflow: "hidden",
      textOverflow: "ellipsis"
    }
  }, "Anna Krause"), /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 10px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Revenue ops \xB7 Admin")), /*#__PURE__*/React.createElement("span", {
    onClick: onTheme,
    title: "Theme",
    style: {
      cursor: "pointer",
      padding: 6,
      borderRadius: 6,
      color: "var(--sp-text-muted)",
      fontSize: 14
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, theme === "dark" ? "☼" : "☾"), !collapsed && /*#__PURE__*/React.createElement("span", {
    onClick: onToggle,
    title: "Collapse",
    style: {
      cursor: "pointer",
      padding: 6,
      borderRadius: 6,
      color: "var(--sp-text-muted)",
      fontSize: 14
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, "\xAB")), collapsed && /*#__PURE__*/React.createElement("span", {
    onClick: onToggle,
    style: {
      cursor: "pointer",
      padding: 6,
      borderRadius: 6,
      textAlign: "center",
      color: "var(--sp-text-muted)",
      fontSize: 14,
      marginTop: 4
    }
  }, "\xBB"));
}
function TopBarP({
  breadcrumbs,
  actions,
  onCmdK,
  onNotify,
  notifyCount = 0,
  onState,
  state,
  stateOptions
}) {
  return /*#__PURE__*/React.createElement("header", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 16,
      padding: "12px 24px",
      background: "var(--sp-surface)",
      borderBottom: "1px solid var(--sp-border)",
      minHeight: 56,
      boxSizing: "border-box"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-muted)",
      display: "flex",
      gap: 6,
      alignItems: "center",
      flexWrap: "nowrap",
      overflow: "hidden"
    }
  }, (breadcrumbs || []).map((c, i) => {
    const last = i === breadcrumbs.length - 1;
    return /*#__PURE__*/React.createElement(React.Fragment, {
      key: i
    }, i > 0 && /*#__PURE__*/React.createElement("span", {
      style: {
        opacity: .4
      }
    }, "\u203A"), /*#__PURE__*/React.createElement("span", {
      style: {
        color: last ? "var(--sp-text)" : "var(--sp-text-muted)",
        fontWeight: last ? 500 : 400,
        whiteSpace: "nowrap"
      }
    }, c));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), stateOptions && /*#__PURE__*/React.createElement(PSegmented, {
    size: "sm",
    options: stateOptions,
    value: state,
    onChange: onState
  }), /*#__PURE__*/React.createElement("div", {
    onClick: onCmdK,
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      padding: "7px 12px",
      border: "1px solid var(--sp-border)",
      borderRadius: 8,
      background: "var(--sp-surface-2)",
      width: 280,
      cursor: "pointer"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontSize: 14
    }
  }, "\u2315"), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, "Search or run command\u2026"), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 10px/14px Roboto",
      color: "var(--sp-text-muted)",
      background: "var(--sp-surface)",
      padding: "2px 6px",
      borderRadius: 4,
      border: "1px solid var(--sp-border)"
    }
  }, "\u2318K")), actions, /*#__PURE__*/React.createElement("span", {
    onClick: onNotify,
    style: {
      position: "relative",
      cursor: "pointer",
      padding: 8,
      borderRadius: 8,
      color: "var(--sp-text-muted)",
      fontSize: 16
    }
  }, "\u2691", notifyCount > 0 && /*#__PURE__*/React.createElement("span", {
    style: {
      position: "absolute",
      top: 4,
      right: 4,
      minWidth: 14,
      height: 14,
      padding: "0 3px",
      borderRadius: 7,
      background: "var(--sp-accent-warm)",
      color: "#fff",
      font: "600 9px/14px Roboto",
      textAlign: "center",
      display: "inline-flex",
      alignItems: "center",
      justifyContent: "center"
    }
  }, notifyCount)));
}

// Right-side drawer
function PDrawer({
  open,
  onClose,
  title,
  width = 520,
  children,
  footer
}) {
  if (!open) return null;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      inset: 0,
      zIndex: 100,
      display: "flex",
      justifyContent: "flex-end"
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: "absolute",
      inset: 0,
      background: "rgba(15,23,42,.35)",
      backdropFilter: "blur(2px)",
      animation: "sp-fade 160ms ease-out"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      width,
      maxWidth: "100%",
      background: "var(--sp-surface)",
      boxShadow: "var(--sp-shadow-float)",
      display: "flex",
      flexDirection: "column",
      animation: "sp-slide 220ms cubic-bezier(.4,0,.2,1)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "16px 20px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      font: "600 15px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, title), /*#__PURE__*/React.createElement("span", {
    onClick: onClose,
    style: {
      cursor: "pointer",
      color: "var(--sp-text-muted)",
      fontSize: 18,
      padding: 4
    }
  }, "\u2715")), /*#__PURE__*/React.createElement("div", {
    className: "sp-scroll",
    style: {
      flex: 1,
      overflow: "auto",
      padding: 20
    }
  }, children), footer && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 20px",
      borderTop: "1px solid var(--sp-border)",
      display: "flex",
      gap: 10,
      justifyContent: "flex-end"
    }
  }, footer)), /*#__PURE__*/React.createElement("style", null, `
        @keyframes sp-fade { from { opacity: 0 } to { opacity: 1 } }
        @keyframes sp-slide { from { transform: translateX(20px); opacity: 0 } to { transform: translateX(0); opacity: 1 } }
      `));
}

// Center modal
function PModal({
  open,
  onClose,
  title,
  children,
  footer,
  width = 440
}) {
  if (!open) return null;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      inset: 0,
      zIndex: 110,
      display: "flex",
      alignItems: "center",
      justifyContent: "center"
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: "absolute",
      inset: 0,
      background: "rgba(15,23,42,.45)",
      animation: "sp-fade 160ms ease-out"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      width,
      maxWidth: "90%",
      borderRadius: 14,
      background: "var(--sp-surface)",
      boxShadow: "var(--sp-shadow-float)",
      animation: "sp-pop 200ms cubic-bezier(.2,.8,.3,1.2)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "18px 22px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      font: "600 16px/22px Roboto",
      color: "var(--sp-text)"
    }
  }, title), /*#__PURE__*/React.createElement("span", {
    onClick: onClose,
    style: {
      cursor: "pointer",
      color: "var(--sp-text-muted)",
      fontSize: 18
    }
  }, "\u2715")), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "20px 22px",
      font: "400 14px/20px Roboto",
      color: "var(--sp-text)"
    }
  }, children), footer && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 22px",
      borderTop: "1px solid var(--sp-border)",
      display: "flex",
      gap: 10,
      justifyContent: "flex-end"
    }
  }, footer)), /*#__PURE__*/React.createElement("style", null, `
        @keyframes sp-pop { from { transform: scale(.95); opacity: 0 } to { transform: scale(1); opacity: 1 } }
      `));
}

// Toast
function PToast({
  toast,
  onDismiss
}) {
  useEffectSh(() => {
    if (!toast) return;
    const t = setTimeout(() => onDismiss?.(), toast.duration || 4000);
    return () => clearTimeout(t);
  }, [toast]);
  if (!toast) return null;
  const tone = toast.tone || "info";
  const colors = {
    info: ["#1A73E8", "rgba(26,115,232,.12)"],
    success: ["var(--sp-accent-mint)", "rgba(0,184,148,.14)"],
    warn: ["#B06000", "rgba(244,180,0,.18)"],
    error: ["#D93025", "rgba(217,48,37,.12)"]
  }[tone];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      bottom: 24,
      right: 24,
      zIndex: 200,
      background: "var(--sp-surface)",
      borderRadius: 10,
      padding: "12px 14px",
      boxShadow: "var(--sp-shadow-float)",
      minWidth: 280,
      maxWidth: 420,
      display: "flex",
      gap: 12,
      alignItems: "flex-start",
      animation: "sp-slide 220ms cubic-bezier(.4,0,.2,1)",
      borderLeft: `3px solid ${colors[0]}`
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      width: 24,
      height: 24,
      borderRadius: "50%",
      background: colors[1],
      color: colors[0],
      font: "600 12px/24px Roboto",
      textAlign: "center",
      flexShrink: 0
    }
  }, tone === "success" ? "✓" : tone === "warn" ? "!" : tone === "error" ? "✕" : "i"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }, toast.title && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)",
      marginBottom: 2
    }
  }, toast.title), toast.body && /*#__PURE__*/React.createElement("div", {
    style: {
      font: "400 12px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, toast.body)), /*#__PURE__*/React.createElement("span", {
    onClick: onDismiss,
    style: {
      cursor: "pointer",
      color: "var(--sp-text-subtle)",
      fontSize: 14,
      padding: "0 4px"
    }
  }, "\u2715"));
}

// Command palette (⌘K)
function PCmdK({
  open,
  onClose,
  onNavigate
}) {
  const [q, setQ] = useStateSh("");
  const items = [{
    k: "nav-subs",
    label: "Go to Subscriptions",
    hint: "Navigate",
    act: () => onNavigate("subscriptions")
  }, {
    k: "nav-inv",
    label: "Go to Invoices",
    hint: "Navigate",
    act: () => onNavigate("invoices")
  }, {
    k: "nav-dun",
    label: "Go to Dunning queue",
    hint: "Navigate",
    act: () => onNavigate("dunning")
  }, {
    k: "nav-plans",
    label: "Go to Plans & pricing",
    hint: "Navigate",
    act: () => onNavigate("plans")
  }, {
    k: "nav-pay",
    label: "Go to Payment methods",
    hint: "Navigate",
    act: () => onNavigate("payments")
  }, {
    k: "nav-chk",
    label: "Start upgrade / checkout",
    hint: "Action",
    act: () => onNavigate("checkout")
  }, {
    k: "act-new-sub",
    label: "Create new subscription",
    hint: "Action · N S",
    act: () => onNavigate("subscriptions")
  }, {
    k: "act-refund",
    label: "Issue refund",
    hint: "Action",
    act: () => {}
  }];
  const filtered = q ? items.filter(i => i.label.toLowerCase().includes(q.toLowerCase())) : items;
  if (!open) return null;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      inset: 0,
      zIndex: 120,
      display: "flex",
      alignItems: "flex-start",
      justifyContent: "center",
      paddingTop: "14vh"
    }
  }, /*#__PURE__*/React.createElement("div", {
    onClick: onClose,
    style: {
      position: "absolute",
      inset: 0,
      background: "rgba(15,23,42,.5)",
      animation: "sp-fade 160ms"
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      position: "relative",
      width: 520,
      maxWidth: "90%",
      borderRadius: 14,
      background: "var(--sp-surface)",
      boxShadow: "var(--sp-shadow-float)",
      overflow: "hidden",
      animation: "sp-pop 220ms cubic-bezier(.2,.8,.3,1.2)"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "14px 18px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      alignItems: "center",
      gap: 10
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontSize: 16
    }
  }, "\u2315"), /*#__PURE__*/React.createElement("input", {
    autoFocus: true,
    value: q,
    onChange: e => setQ(e.target.value),
    placeholder: "Type a command or search\u2026",
    style: {
      flex: 1,
      border: "none",
      outline: "none",
      background: "transparent",
      font: "400 15px/22px Roboto",
      color: "var(--sp-text)"
    }
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 10px/14px Roboto",
      color: "var(--sp-text-subtle)",
      padding: "2px 6px",
      borderRadius: 4,
      border: "1px solid var(--sp-border)"
    }
  }, "esc")), /*#__PURE__*/React.createElement("div", {
    className: "sp-scroll",
    style: {
      maxHeight: 360,
      overflow: "auto",
      padding: 6
    }
  }, filtered.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 28,
      textAlign: "center",
      color: "var(--sp-text-subtle)",
      font: "400 13px/18px Roboto"
    }
  }, "No results"), filtered.map((it, i) => /*#__PURE__*/React.createElement("div", {
    key: it.k,
    onClick: () => {
      it.act();
      onClose();
    },
    style: {
      padding: "10px 14px",
      borderRadius: 8,
      cursor: "pointer",
      display: "flex",
      alignItems: "center",
      gap: 10,
      font: "400 13px/18px Roboto",
      color: "var(--sp-text)"
    },
    onMouseEnter: e => e.currentTarget.style.background = "var(--sp-surface-2)",
    onMouseLeave: e => e.currentTarget.style.background = "transparent"
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      color: "var(--sp-text-muted)",
      fontSize: 14
    }
  }, "\u203A"), /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1
    }
  }, it.label), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-subtle)"
    }
  }, it.hint))))));
}
Object.assign(window, {
  NAV,
  SideNavP,
  TopBarP,
  PDrawer,
  PModal,
  PToast,
  PCmdK
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/sub_Shell.jsx", error: String((e && e.message) || e) }); }

// ui_kits/crm-web/template_designer.jsx
try { (() => {
// ═══════════════════════════════════════════════════════════════════════
// Template Designer — block-based visual email composer
//
// Opens as a full-screen overlay from TemplatesP. State lives locally;
// onSave returns the document, onCancel discards.
//
// Architecture
// ────────────
//   doc = {
//     id, name, channel, lang, subject, preheader,
//     brand: { color, name, logoText },
//     blocks: [ { id, type, props } ]
//   }
//
//   types: header | text | button | image | divider | spacer | columns | footer
//
// Three panes: Library (drag from) · Canvas (drag-reorder, click-select)
// · Inspector (edit selected block).
// ═══════════════════════════════════════════════════════════════════════

const {
  useState: useStTD,
  useRef: useRefTD,
  useEffect: useEffectTD
} = React;

// ── Block defaults ─────────────────────────────────────────────────────
const BLOCK_DEFAULTS = {
  header: {
    label: "Header",
    icon: "▤",
    props: {
      logoText: "Incedo",
      bg: "#1A73E8",
      fg: "#FFFFFF",
      align: "left",
      padding: 16
    }
  },
  text: {
    label: "Text",
    icon: "T",
    props: {
      content: "Edit this text. Use {{merge.tags}} to personalize.",
      color: "#202124",
      size: 14,
      align: "left",
      padding: 16
    }
  },
  button: {
    label: "Button",
    icon: "▭",
    props: {
      label: "View invoice",
      href: "{{invoice.url}}",
      bg: "#1A73E8",
      fg: "#FFFFFF",
      align: "center",
      radius: 6,
      padding: 16,
      fullWidth: false
    }
  },
  image: {
    label: "Image",
    icon: "▣",
    props: {
      src: "",
      alt: "Image",
      width: 100,
      align: "center",
      padding: 16,
      href: ""
    }
  },
  divider: {
    label: "Divider",
    icon: "─",
    props: {
      color: "#E8EAED",
      thickness: 1,
      padding: 12
    }
  },
  spacer: {
    label: "Spacer",
    icon: "↕",
    props: {
      height: 24
    }
  },
  columns: {
    label: "Two columns",
    icon: "▥",
    props: {
      left: "Left column copy.",
      right: "Right column copy.",
      color: "#202124",
      size: 13,
      padding: 16,
      gap: 16
    }
  },
  footer: {
    label: "Footer",
    icon: "▤",
    props: {
      content: "Incedo B.V. · Herengracht 124 · Amsterdam\nUnsubscribe (admin emails are still required)",
      color: "#80868B",
      size: 11,
      align: "center",
      padding: 20,
      bg: "#F8F9FA"
    }
  }
};
const BLOCK_ORDER = ["header", "text", "button", "image", "divider", "spacer", "columns", "footer"];

// Merge-tag chips shown in the inspector on text-bearing blocks.
const MERGE_TAGS = [["{{customer.name}}", "Customer name"], ["{{customer.email}}", "Customer email"], ["{{invoice.id}}", "Invoice ID"], ["{{invoice.amount}}", "Amount"], ["{{invoice.due}}", "Due date"], ["{{invoice.url}}", "Invoice link"], ["{{invoice.method}}", "Payment method"], ["{{payment.error}}", "Failure reason"], ["{{plan.name}}", "Plan name"], ["{{sub.next}}", "Next renewal"], ["{{portal.url}}", "Portal link"]];

// Sample resolved values for the preview "rendered" mode.
const SAMPLE_VALUES = {
  "{{customer.name}}": "Acme Holdings",
  "{{customer.email}}": "billing@acme.example",
  "{{invoice.id}}": "INV-20260507",
  "{{invoice.amount}}": "€49,900.00",
  "{{invoice.due}}": "May 21, 2026",
  "{{invoice.url}}": "portal.incedo.com/i/INV-…",
  "{{invoice.method}}": "Visa •• 4242",
  "{{payment.error}}": "insufficient_funds",
  "{{plan.name}}": "Enterprise",
  "{{sub.next}}": "Jun 14, 2026",
  "{{portal.url}}": "portal.incedo.com/u/abc"
};
const renderTags = (s, mode) => {
  if (!s || mode === "raw") return s;
  return s.replace(/\{\{[\w.]+\}\}/g, m => SAMPLE_VALUES[m] ?? m);
};
const uid = () => "b" + Math.random().toString(36).slice(2, 9);

// Starter doc when "New template" is clicked.
const blankDoc = () => ({
  id: "tpl_new_" + Date.now(),
  name: "Untitled template",
  channel: "email",
  lang: "en",
  subject: "Subject line",
  preheader: "Short preview text shown in the inbox list.",
  brand: {
    color: "#1A73E8",
    name: "Incedo",
    logoText: "Incedo"
  },
  blocks: [{
    id: uid(),
    type: "header",
    props: {
      ...BLOCK_DEFAULTS.header.props
    }
  }, {
    id: uid(),
    type: "text",
    props: {
      ...BLOCK_DEFAULTS.text.props,
      content: "Hi {{customer.name}},\n\nWrite your message here."
    }
  }, {
    id: uid(),
    type: "button",
    props: {
      ...BLOCK_DEFAULTS.button.props
    }
  }, {
    id: uid(),
    type: "footer",
    props: {
      ...BLOCK_DEFAULTS.footer.props
    }
  }]
});

// Convert an existing TEMPLATES row into a designer doc (reverse-engineered
// from its body string — best-effort, gives the user a starting point).
const docFromTemplate = (tpl, body) => ({
  id: tpl.id,
  name: tpl.name,
  channel: tpl.channel,
  lang: tpl.lang,
  subject: tpl.subject,
  preheader: tpl.preheader || "",
  brand: {
    color: "#1A73E8",
    name: "Incedo",
    logoText: "Incedo"
  },
  blocks: [{
    id: uid(),
    type: "header",
    props: {
      ...BLOCK_DEFAULTS.header.props
    }
  }, {
    id: uid(),
    type: "text",
    props: {
      ...BLOCK_DEFAULTS.text.props,
      content: body || ""
    }
  }, {
    id: uid(),
    type: "button",
    props: {
      ...BLOCK_DEFAULTS.button.props
    }
  }, {
    id: uid(),
    type: "footer",
    props: {
      ...BLOCK_DEFAULTS.footer.props
    }
  }]
});

// ────────────────────────────────────────────────────────────────────────
// Block renderers — pure presentational
// ────────────────────────────────────────────────────────────────────────
function BlockRender({
  b,
  mode = "rendered"
}) {
  const p = b.props;
  switch (b.type) {
    case "header":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          background: p.bg,
          color: p.fg,
          padding: p.padding,
          textAlign: p.align,
          font: "700 18px/24px Roboto"
        }
      }, /*#__PURE__*/React.createElement("span", {
        style: {
          display: "inline-flex",
          alignItems: "center",
          gap: 8
        }
      }, /*#__PURE__*/React.createElement("span", {
        style: {
          width: 24,
          height: 24,
          borderRadius: 6,
          background: "rgba(255,255,255,.18)",
          display: "inline-grid",
          placeItems: "center",
          fontSize: 12
        }
      }, "i"), p.logoText));
    case "text":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: p.padding,
          color: p.color,
          font: `400 ${p.size}px/${Math.round(p.size * 1.5)}px Roboto`,
          textAlign: p.align,
          whiteSpace: "pre-wrap"
        }
      }, renderTags(p.content, mode));
    case "button":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: p.padding,
          textAlign: p.align
        }
      }, /*#__PURE__*/React.createElement("span", {
        style: {
          display: p.fullWidth ? "block" : "inline-block",
          background: p.bg,
          color: p.fg,
          padding: "10px 20px",
          borderRadius: p.radius,
          font: "600 14px/20px Roboto",
          textDecoration: "none"
        }
      }, p.label));
    case "image":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: p.padding,
          textAlign: p.align
        }
      }, p.src ? /*#__PURE__*/React.createElement("img", {
        src: p.src,
        alt: p.alt,
        style: {
          width: `${p.width}%`,
          maxWidth: "100%",
          display: "inline-block",
          borderRadius: 4
        }
      }) : /*#__PURE__*/React.createElement("div", {
        style: {
          width: `${p.width}%`,
          margin: p.align === "center" ? "0 auto" : 0,
          aspectRatio: "16/9",
          background: "repeating-linear-gradient(45deg, #F1F3F4 0 8px, #E8EAED 8px 16px)",
          border: "1px dashed #DADCE0",
          borderRadius: 4,
          display: "grid",
          placeItems: "center",
          color: "#80868B",
          font: "500 12px/16px Roboto"
        }
      }, "Image placeholder"));
    case "divider":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: `${p.padding}px 16px`
        }
      }, /*#__PURE__*/React.createElement("div", {
        style: {
          borderTop: `${p.thickness}px solid ${p.color}`
        }
      }));
    case "spacer":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          height: p.height
        }
      });
    case "columns":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: p.padding,
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: p.gap,
          color: p.color,
          font: `400 ${p.size}px/${Math.round(p.size * 1.5)}px Roboto`
        }
      }, /*#__PURE__*/React.createElement("div", {
        style: {
          whiteSpace: "pre-wrap"
        }
      }, renderTags(p.left, mode)), /*#__PURE__*/React.createElement("div", {
        style: {
          whiteSpace: "pre-wrap"
        }
      }, renderTags(p.right, mode)));
    case "footer":
      return /*#__PURE__*/React.createElement("div", {
        style: {
          padding: p.padding,
          background: p.bg,
          color: p.color,
          font: `400 ${p.size}px/${Math.round(p.size * 1.4)}px Roboto`,
          textAlign: p.align,
          whiteSpace: "pre-wrap"
        }
      }, p.content);
    default:
      return null;
  }
}

// ────────────────────────────────────────────────────────────────────────
// Library — left column (draggable items)
// ────────────────────────────────────────────────────────────────────────
function Library({
  onAdd
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: 200,
      borderRight: "1px solid var(--sp-border)",
      background: "var(--sp-surface)",
      padding: 12,
      overflowY: "auto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      textTransform: "uppercase",
      letterSpacing: ".08em",
      marginBottom: 10
    }
  }, "Blocks"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "grid",
      gridTemplateColumns: "1fr 1fr",
      gap: 8
    }
  }, BLOCK_ORDER.map(t => {
    const def = BLOCK_DEFAULTS[t];
    return /*#__PURE__*/React.createElement("div", {
      key: t,
      draggable: true,
      onDragStart: e => {
        e.dataTransfer.setData("text/td-new", t);
        e.dataTransfer.effectAllowed = "copy";
      },
      onClick: () => onAdd(t),
      style: {
        padding: "10px 8px",
        background: "var(--sp-canvas)",
        border: "1px solid var(--sp-border)",
        borderRadius: 6,
        cursor: "grab",
        textAlign: "center",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        gap: 4
      },
      onMouseEnter: e => e.currentTarget.style.borderColor = "#1A73E8",
      onMouseLeave: e => e.currentTarget.style.borderColor = "var(--sp-border)"
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 16px/18px Roboto",
        color: "var(--sp-text)"
      }
    }, def.icon), /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)"
      }
    }, def.label));
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 16,
      padding: 10,
      background: "var(--sp-canvas)",
      border: "1px dashed var(--sp-border)",
      borderRadius: 6,
      font: "400 11px/16px Roboto",
      color: "var(--sp-text-muted)"
    }
  }, "Drag a block onto the canvas, or click to append."));
}

// ────────────────────────────────────────────────────────────────────────
// Canvas — middle column
// ────────────────────────────────────────────────────────────────────────
function Canvas({
  doc,
  selectedId,
  onSelect,
  onMove,
  onDrop,
  onDelete,
  onDuplicate,
  viewMode,
  device
}) {
  const dragId = useRefTD(null);
  const overIdx = useRefTD(null);
  const [overState, setOverState] = useStTD({
    idx: -1,
    pos: "before"
  });
  const handleDragStart = (id, e) => {
    dragId.current = id;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/td-move", id);
  };
  const handleDragOver = (idx, e) => {
    e.preventDefault();
    const r = e.currentTarget.getBoundingClientRect();
    const pos = e.clientY - r.top < r.height / 2 ? "before" : "after";
    overIdx.current = {
      idx,
      pos
    };
    setOverState({
      idx,
      pos
    });
  };
  const handleDrop = (idx, e) => {
    e.preventDefault();
    const newType = e.dataTransfer.getData("text/td-new");
    const moveId = e.dataTransfer.getData("text/td-move");
    const o = overIdx.current || {
      idx,
      pos: "before"
    };
    const insertAt = o.pos === "before" ? o.idx : o.idx + 1;
    if (newType) onDrop(newType, insertAt);else if (moveId) onMove(moveId, insertAt);
    overIdx.current = null;
    setOverState({
      idx: -1,
      pos: "before"
    });
  };
  const handleEmptyDrop = e => {
    e.preventDefault();
    const newType = e.dataTransfer.getData("text/td-new");
    if (newType) onDrop(newType, doc.blocks.length);
  };
  const canvasWidth = device === "mobile" ? 360 : 600;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      background: "var(--sp-canvas)",
      overflowY: "auto",
      padding: "24px 16px"
    },
    onDragOver: e => {
      if (doc.blocks.length === 0) e.preventDefault();
    },
    onDrop: doc.blocks.length === 0 ? handleEmptyDrop : undefined
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: canvasWidth,
      margin: "0 auto",
      background: "#fff",
      boxShadow: "0 1px 3px rgba(0,0,0,.08), 0 0 0 1px rgba(0,0,0,.04)",
      borderRadius: 8,
      overflow: "hidden",
      transition: "max-width .25s ease"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "10px 16px",
      background: "#F8F9FA",
      borderBottom: "1px solid #E8EAED",
      font: "400 11px/16px Roboto",
      color: "#5F6368"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "600 13px/18px Roboto",
      color: "#202124"
    }
  }, doc.subject || /*#__PURE__*/React.createElement("span", {
    style: {
      color: "#BDC1C6"
    }
  }, "Subject line")), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 2
    }
  }, renderTags(doc.preheader, viewMode) || /*#__PURE__*/React.createElement("span", {
    style: {
      color: "#BDC1C6"
    }
  }, "Preheader"))), doc.blocks.length === 0 && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 60,
      textAlign: "center",
      color: "#80868B",
      font: "500 13px/20px Roboto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 32,
      marginBottom: 8
    }
  }, "\uFF0B"), "Drop a block here to start building."), doc.blocks.map((b, i) => {
    const sel = selectedId === b.id;
    const showLineBefore = overState.idx === i && overState.pos === "before";
    const showLineAfter = overState.idx === i && overState.pos === "after";
    return /*#__PURE__*/React.createElement("div", {
      key: b.id,
      onClick: e => {
        e.stopPropagation();
        onSelect(b.id);
      },
      onDragOver: e => handleDragOver(i, e),
      onDragLeave: () => setOverState({
        idx: -1,
        pos: "before"
      }),
      onDrop: e => handleDrop(i, e),
      style: {
        position: "relative",
        outline: sel ? "2px solid #1A73E8" : "none",
        outlineOffset: -2,
        cursor: "pointer"
      }
    }, showLineBefore && /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: -1,
        left: 0,
        right: 0,
        height: 3,
        background: "#1A73E8",
        zIndex: 5
      }
    }), /*#__PURE__*/React.createElement(BlockRender, {
      b: b,
      mode: viewMode
    }), showLineAfter && /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        bottom: -1,
        left: 0,
        right: 0,
        height: 3,
        background: "#1A73E8",
        zIndex: 5
      }
    }), sel && /*#__PURE__*/React.createElement("div", {
      style: {
        position: "absolute",
        top: -28,
        right: 0,
        display: "flex",
        gap: 4,
        alignItems: "center",
        padding: "4px 6px",
        background: "#1A73E8",
        borderRadius: "4px 4px 0 0",
        font: "500 11px/14px Roboto",
        color: "#fff"
      },
      onClick: e => e.stopPropagation()
    }, /*#__PURE__*/React.createElement("span", {
      draggable: true,
      onDragStart: e => handleDragStart(b.id, e),
      style: {
        padding: "2px 6px",
        cursor: "grab",
        borderRadius: 3
      },
      onMouseEnter: e => e.currentTarget.style.background = "rgba(255,255,255,.18)",
      onMouseLeave: e => e.currentTarget.style.background = "transparent"
    }, "\u22EE\u22EE ", BLOCK_DEFAULTS[b.type]?.label || b.type), /*#__PURE__*/React.createElement("span", {
      onClick: () => onDuplicate(b.id),
      style: {
        padding: "2px 6px",
        cursor: "pointer",
        borderLeft: "1px solid rgba(255,255,255,.25)"
      },
      title: "Duplicate"
    }, "\u2398"), /*#__PURE__*/React.createElement("span", {
      onClick: () => onDelete(b.id),
      style: {
        padding: "2px 6px",
        cursor: "pointer",
        borderLeft: "1px solid rgba(255,255,255,.25)"
      },
      title: "Delete"
    }, "\u2715")));
  }), doc.blocks.length > 0 && /*#__PURE__*/React.createElement("div", {
    onDragOver: e => e.preventDefault(),
    onDrop: handleEmptyDrop,
    style: {
      height: 32,
      borderTop: "1px dashed transparent"
    }
  })));
}

// ────────────────────────────────────────────────────────────────────────
// Inspector — right column
// ────────────────────────────────────────────────────────────────────────
function FieldRow({
  label,
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      marginBottom: 12
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 4
    }
  }, label), children);
}
const inputCss = {
  width: "100%",
  padding: "6px 8px",
  border: "1px solid var(--sp-border)",
  borderRadius: 4,
  background: "var(--sp-surface)",
  color: "var(--sp-text)",
  font: "400 13px/18px Roboto"
};
const TextField = ({
  value,
  onChange,
  placeholder
}) => /*#__PURE__*/React.createElement("input", {
  value: value ?? "",
  placeholder: placeholder,
  onChange: e => onChange(e.target.value),
  style: inputCss
});
const TextArea = ({
  value,
  onChange,
  rows = 4
}) => /*#__PURE__*/React.createElement("textarea", {
  value: value ?? "",
  rows: rows,
  onChange: e => onChange(e.target.value),
  style: {
    ...inputCss,
    resize: "vertical",
    font: "400 13px/18px Roboto"
  }
});
const NumField = ({
  value,
  onChange,
  min = 0,
  max = 200,
  step = 1
}) => /*#__PURE__*/React.createElement("input", {
  type: "number",
  value: value ?? 0,
  min: min,
  max: max,
  step: step,
  onChange: e => onChange(Number(e.target.value)),
  style: inputCss
});
const ColorField = ({
  value,
  onChange
}) => /*#__PURE__*/React.createElement("div", {
  style: {
    display: "flex",
    gap: 6,
    alignItems: "center"
  }
}, /*#__PURE__*/React.createElement("input", {
  type: "color",
  value: value ?? "#000000",
  onChange: e => onChange(e.target.value),
  style: {
    width: 32,
    height: 30,
    padding: 0,
    border: "1px solid var(--sp-border)",
    borderRadius: 4,
    background: "none"
  }
}), /*#__PURE__*/React.createElement("input", {
  value: value ?? "",
  onChange: e => onChange(e.target.value),
  style: {
    ...inputCss,
    font: "400 12px/18px 'Roboto Mono', monospace"
  }
}));
const Segmented = ({
  value,
  onChange,
  options
}) => /*#__PURE__*/React.createElement("div", {
  style: {
    display: "flex",
    border: "1px solid var(--sp-border)",
    borderRadius: 4,
    overflow: "hidden"
  }
}, options.map(o => /*#__PURE__*/React.createElement("span", {
  key: o.value,
  onClick: () => onChange(o.value),
  style: {
    flex: 1,
    textAlign: "center",
    padding: "6px 8px",
    cursor: "pointer",
    background: value === o.value ? "#1A73E8" : "var(--sp-surface)",
    color: value === o.value ? "#fff" : "var(--sp-text)",
    font: "500 12px/16px Roboto",
    borderRight: "1px solid var(--sp-border)"
  }
}, o.label)));
function MergeTagPicker({
  onInsert
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 6
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      font: "500 11px/14px Roboto",
      color: "var(--sp-text-muted)",
      marginBottom: 4
    }
  }, "Insert merge tag"), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      flexWrap: "wrap",
      gap: 4
    }
  }, MERGE_TAGS.map(([k]) => /*#__PURE__*/React.createElement("span", {
    key: k,
    onClick: () => onInsert(k),
    style: {
      padding: "3px 6px",
      border: "1px solid var(--sp-border)",
      borderRadius: 3,
      background: "var(--sp-canvas)",
      cursor: "pointer",
      font: "500 11px/14px 'Roboto Mono', monospace",
      color: "#1A73E8"
    }
  }, k))));
}
function Inspector({
  doc,
  onDocChange,
  selected,
  onUpdate
}) {
  if (!selected) {
    return /*#__PURE__*/React.createElement("div", {
      style: {
        width: 280,
        borderLeft: "1px solid var(--sp-border)",
        background: "var(--sp-surface)",
        overflowY: "auto"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        padding: 16,
        borderBottom: "1px solid var(--sp-border)"
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        textTransform: "uppercase",
        letterSpacing: ".08em",
        marginBottom: 10
      }
    }, "Email settings"), /*#__PURE__*/React.createElement(FieldRow, {
      label: "Subject"
    }, /*#__PURE__*/React.createElement(TextField, {
      value: doc.subject,
      onChange: v => onDocChange({
        ...doc,
        subject: v
      })
    })), /*#__PURE__*/React.createElement(FieldRow, {
      label: "Preheader"
    }, /*#__PURE__*/React.createElement(TextField, {
      value: doc.preheader,
      onChange: v => onDocChange({
        ...doc,
        preheader: v
      })
    })), /*#__PURE__*/React.createElement(FieldRow, {
      label: "Language"
    }, /*#__PURE__*/React.createElement(Segmented, {
      value: doc.lang,
      onChange: v => onDocChange({
        ...doc,
        lang: v
      }),
      options: [{
        value: "en",
        label: "EN"
      }, {
        value: "nl",
        label: "NL"
      }]
    }))), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: 16
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        font: "500 11px/14px Roboto",
        color: "var(--sp-text-muted)",
        textTransform: "uppercase",
        letterSpacing: ".08em",
        marginBottom: 10
      }
    }, "Brand"), /*#__PURE__*/React.createElement(FieldRow, {
      label: "Brand color"
    }, /*#__PURE__*/React.createElement(ColorField, {
      value: doc.brand.color,
      onChange: v => onDocChange({
        ...doc,
        brand: {
          ...doc.brand,
          color: v
        }
      })
    })), /*#__PURE__*/React.createElement(FieldRow, {
      label: "Logo text"
    }, /*#__PURE__*/React.createElement(TextField, {
      value: doc.brand.logoText,
      onChange: v => onDocChange({
        ...doc,
        brand: {
          ...doc.brand,
          logoText: v
        }
      })
    }))), /*#__PURE__*/React.createElement("div", {
      style: {
        padding: 16,
        borderTop: "1px solid var(--sp-border)",
        color: "var(--sp-text-muted)",
        font: "400 12px/18px Roboto"
      }
    }, "Click a block on the canvas to edit it."));
  }
  const p = selected.props;
  const set = patch => onUpdate(selected.id, {
    ...p,
    ...patch
  });
  const Common = {
    padding: /*#__PURE__*/React.createElement(FieldRow, {
      label: "Padding (px)"
    }, /*#__PURE__*/React.createElement(NumField, {
      value: p.padding,
      min: 0,
      max: 64,
      onChange: v => set({
        padding: v
      })
    })),
    align: /*#__PURE__*/React.createElement(FieldRow, {
      label: "Alignment"
    }, /*#__PURE__*/React.createElement(Segmented, {
      value: p.align,
      onChange: v => set({
        align: v
      }),
      options: [{
        value: "left",
        label: "L"
      }, {
        value: "center",
        label: "C"
      }, {
        value: "right",
        label: "R"
      }]
    }))
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width: 280,
      borderLeft: "1px solid var(--sp-border)",
      background: "var(--sp-surface)",
      overflowY: "auto"
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      padding: "12px 16px",
      borderBottom: "1px solid var(--sp-border)",
      display: "flex",
      justifyContent: "space-between",
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      font: "600 13px/18px Roboto",
      color: "var(--sp-text)"
    }
  }, BLOCK_DEFAULTS[selected.type]?.label || selected.type), /*#__PURE__*/React.createElement("span", {
    style: {
      font: "400 11px/14px 'Roboto Mono', monospace",
      color: "var(--sp-text-muted)"
    }
  }, selected.type)), /*#__PURE__*/React.createElement("div", {
    style: {
      padding: 16
    }
  }, selected.type === "header" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Logo text"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.logoText,
    onChange: v => set({
      logoText: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Background"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.bg,
    onChange: v => set({
      bg: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Foreground"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.fg,
    onChange: v => set({
      fg: v
    })
  })), Common.align, Common.padding), selected.type === "text" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Content"
  }, /*#__PURE__*/React.createElement(TextArea, {
    value: p.content,
    rows: 6,
    onChange: v => set({
      content: v
    })
  })), /*#__PURE__*/React.createElement(MergeTagPicker, {
    onInsert: t => set({
      content: (p.content || "") + " " + t
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Color"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.color,
    onChange: v => set({
      color: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Size (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.size,
    min: 10,
    max: 32,
    onChange: v => set({
      size: v
    })
  })), Common.align, Common.padding), selected.type === "button" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Label"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.label,
    onChange: v => set({
      label: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "URL"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.href,
    placeholder: "{{portal.url}}",
    onChange: v => set({
      href: v
    })
  })), /*#__PURE__*/React.createElement(MergeTagPicker, {
    onInsert: t => set({
      href: t
    })
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      height: 12
    }
  }), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Background"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.bg,
    onChange: v => set({
      bg: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Foreground"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.fg,
    onChange: v => set({
      fg: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Radius (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.radius,
    min: 0,
    max: 32,
    onChange: v => set({
      radius: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Full width"
  }, /*#__PURE__*/React.createElement(Segmented, {
    value: p.fullWidth ? "on" : "off",
    onChange: v => set({
      fullWidth: v === "on"
    }),
    options: [{
      value: "off",
      label: "Auto"
    }, {
      value: "on",
      label: "Full"
    }]
  })), Common.align, Common.padding), selected.type === "image" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Image URL"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.src,
    placeholder: "https://\u2026",
    onChange: v => set({
      src: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Alt text"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.alt,
    onChange: v => set({
      alt: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Link URL"
  }, /*#__PURE__*/React.createElement(TextField, {
    value: p.href,
    onChange: v => set({
      href: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Width (%)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.width,
    min: 20,
    max: 100,
    step: 5,
    onChange: v => set({
      width: v
    })
  })), Common.align, Common.padding), selected.type === "divider" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Color"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.color,
    onChange: v => set({
      color: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Thickness (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.thickness,
    min: 1,
    max: 8,
    onChange: v => set({
      thickness: v
    })
  })), Common.padding), selected.type === "spacer" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Height (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.height,
    min: 4,
    max: 120,
    onChange: v => set({
      height: v
    })
  }))), selected.type === "columns" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Left column"
  }, /*#__PURE__*/React.createElement(TextArea, {
    value: p.left,
    rows: 4,
    onChange: v => set({
      left: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Right column"
  }, /*#__PURE__*/React.createElement(TextArea, {
    value: p.right,
    rows: 4,
    onChange: v => set({
      right: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Color"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.color,
    onChange: v => set({
      color: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Size (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.size,
    min: 10,
    max: 20,
    onChange: v => set({
      size: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Gap (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.gap,
    min: 0,
    max: 32,
    onChange: v => set({
      gap: v
    })
  })), Common.padding), selected.type === "footer" && /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(FieldRow, {
    label: "Content"
  }, /*#__PURE__*/React.createElement(TextArea, {
    value: p.content,
    rows: 4,
    onChange: v => set({
      content: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Background"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.bg,
    onChange: v => set({
      bg: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Color"
  }, /*#__PURE__*/React.createElement(ColorField, {
    value: p.color,
    onChange: v => set({
      color: v
    })
  })), /*#__PURE__*/React.createElement(FieldRow, {
    label: "Size (px)"
  }, /*#__PURE__*/React.createElement(NumField, {
    value: p.size,
    min: 9,
    max: 16,
    onChange: v => set({
      size: v
    })
  })), Common.align, Common.padding)));
}

// ────────────────────────────────────────────────────────────────────────
// Top bar
// ────────────────────────────────────────────────────────────────────────
function DesignerTopBar({
  doc,
  onDocChange,
  viewMode,
  onViewMode,
  device,
  onDevice,
  onCancel,
  onSave,
  onTest
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 12,
      padding: "10px 16px",
      borderBottom: "1px solid var(--sp-border)",
      background: "var(--sp-surface)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    onClick: onCancel,
    style: {
      cursor: "pointer",
      color: "var(--sp-text-muted)",
      font: "500 13px/18px Roboto",
      padding: "4px 8px"
    }
  }, "\u2190 Back"), /*#__PURE__*/React.createElement("input", {
    value: doc.name,
    onChange: e => onDocChange({
      ...doc,
      name: e.target.value
    }),
    style: {
      flex: "0 1 320px",
      padding: "6px 10px",
      border: "1px solid transparent",
      borderRadius: 4,
      background: "transparent",
      color: "var(--sp-text)",
      font: "600 15px/22px Roboto"
    },
    onMouseEnter: e => e.currentTarget.style.borderColor = "var(--sp-border)",
    onMouseLeave: e => e.currentTarget.style.borderColor = "transparent"
  }), /*#__PURE__*/React.createElement("span", {
    style: {
      padding: "2px 8px",
      borderRadius: 10,
      background: "rgba(26,115,232,.1)",
      color: "#1A73E8",
      font: "500 11px/16px Roboto",
      textTransform: "uppercase",
      letterSpacing: ".06em"
    }
  }, doc.channel), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), /*#__PURE__*/React.createElement(Segmented, {
    value: viewMode,
    onChange: onViewMode,
    options: [{
      value: "rendered",
      label: "Rendered"
    }, {
      value: "raw",
      label: "Raw tags"
    }]
  }), /*#__PURE__*/React.createElement(Segmented, {
    value: device,
    onChange: onDevice,
    options: [{
      value: "desktop",
      label: "▭"
    }, {
      value: "mobile",
      label: "▯"
    }]
  }), /*#__PURE__*/React.createElement(PButton, {
    variant: "secondary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2709"
    }),
    onClick: onTest
  }, "Test send"), /*#__PURE__*/React.createElement(PButton, {
    variant: "ghost",
    size: "sm",
    onClick: onCancel
  }, "Cancel"), /*#__PURE__*/React.createElement(PButton, {
    variant: "primary",
    size: "sm",
    leading: /*#__PURE__*/React.createElement(Ico, {
      g: "\u2713"
    }),
    onClick: onSave
  }, "Save"));
}

// ────────────────────────────────────────────────────────────────────────
// TemplateDesigner — top-level component
// ────────────────────────────────────────────────────────────────────────
function TemplateDesigner({
  initialDoc,
  onSave,
  onCancel
}) {
  const [doc, setDoc] = useStTD(initialDoc || blankDoc());
  const [selectedId, setSelectedId] = useStTD(null);
  const [viewMode, setViewMode] = useStTD("rendered");
  const [device, setDevice] = useStTD("desktop");
  const [toast, setToast] = useStTD(null);

  // Helpers
  const updateProps = (id, props) => setDoc(d => ({
    ...d,
    blocks: d.blocks.map(b => b.id === id ? {
      ...b,
      props
    } : b)
  }));
  const insertBlock = (type, idx) => {
    const block = {
      id: uid(),
      type,
      props: {
        ...BLOCK_DEFAULTS[type].props
      }
    };
    setDoc(d => {
      const next = [...d.blocks];
      next.splice(idx ?? next.length, 0, block);
      return {
        ...d,
        blocks: next
      };
    });
    setSelectedId(block.id);
  };
  const moveBlock = (id, idx) => {
    setDoc(d => {
      const cur = d.blocks.findIndex(b => b.id === id);
      if (cur < 0) return d;
      const block = d.blocks[cur];
      const next = d.blocks.filter(b => b.id !== id);
      const target = idx > cur ? idx - 1 : idx;
      next.splice(target, 0, block);
      return {
        ...d,
        blocks: next
      };
    });
  };
  const deleteBlock = id => {
    setDoc(d => ({
      ...d,
      blocks: d.blocks.filter(b => b.id !== id)
    }));
    if (selectedId === id) setSelectedId(null);
  };
  const duplicateBlock = id => {
    setDoc(d => {
      const idx = d.blocks.findIndex(b => b.id === id);
      if (idx < 0) return d;
      const copy = {
        ...d.blocks[idx],
        id: uid()
      };
      const next = [...d.blocks];
      next.splice(idx + 1, 0, copy);
      return {
        ...d,
        blocks: next
      };
    });
  };
  const selected = doc.blocks.find(b => b.id === selectedId) || null;
  const flash = m => {
    setToast(m);
    setTimeout(() => setToast(null), 2000);
  };
  return /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      inset: 0,
      zIndex: 100,
      background: "var(--sp-canvas)",
      display: "flex",
      flexDirection: "column"
    },
    onClick: () => setSelectedId(null)
  }, /*#__PURE__*/React.createElement(DesignerTopBar, {
    doc: doc,
    onDocChange: setDoc,
    viewMode: viewMode,
    onViewMode: setViewMode,
    device: device,
    onDevice: setDevice,
    onCancel: onCancel,
    onSave: () => {
      onSave(doc);
      flash("Template saved");
    },
    onTest: () => flash("Test email queued to billing@incedo.example")
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      display: "flex",
      minHeight: 0
    },
    onClick: e => e.stopPropagation()
  }, /*#__PURE__*/React.createElement(Library, {
    onAdd: t => insertBlock(t)
  }), /*#__PURE__*/React.createElement(Canvas, {
    doc: doc,
    selectedId: selectedId,
    onSelect: setSelectedId,
    onMove: moveBlock,
    onDrop: insertBlock,
    onDelete: deleteBlock,
    onDuplicate: duplicateBlock,
    viewMode: viewMode,
    device: device
  }), /*#__PURE__*/React.createElement(Inspector, {
    doc: doc,
    onDocChange: setDoc,
    selected: selected,
    onUpdate: updateProps
  })), toast && /*#__PURE__*/React.createElement("div", {
    style: {
      position: "fixed",
      bottom: 24,
      left: "50%",
      transform: "translateX(-50%)",
      background: "#202124",
      color: "#fff",
      padding: "10px 16px",
      borderRadius: 6,
      font: "500 13px/18px Roboto",
      zIndex: 200,
      boxShadow: "0 4px 12px rgba(0,0,0,.2)"
    }
  }, toast));
}

// Exports — TemplatesP wraps this; see billing_extras.jsx
Object.assign(window, {
  TemplateDesigner,
  blankDoc,
  docFromTemplate,
  BLOCK_DEFAULTS
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/crm-web/template_designer.jsx", error: String((e && e.message) || e) }); }

})();
