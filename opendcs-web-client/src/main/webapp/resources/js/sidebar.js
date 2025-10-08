
/*!
 * OpenDCS helpers (Bootstrap 5)
 * - NOTE - I consider this another SHIM
 * - Sidebar collapse + accordion groups
 * - Stacked modals fix (nested modals)
 * - Action dropdowns in DataTables: programmatic toggle + stop row handlers
 * - Fallback: show_waiting_modal / hide_waiting_modal
 */
(function () {
  function whenReady(cb) {
    if (document.readyState !== 'loading') cb();
    else document.addEventListener('DOMContentLoaded', cb, { once: true });
  }
  function directChild(parent, selector) {
    if (!parent) return null;
    for (var i = 0; i < parent.children.length; i++) {
      var el = parent.children[i];
      if (el.matches && el.matches(selector)) return el;
    }
    return null;
  }
  function isDisplayed(el) {
    if (!el) return false;
    if (el.style && el.style.display) return el.style.display !== 'none';
    try { return window.getComputedStyle(el).display !== 'none'; } catch (e) { return true; }
  }

  // ---------------- Fallback waiting modal ----------------
  function ensureWaitingModal() {
    var id = 'opendcs-waiting-modal';
    var el = document.getElementById(id);
    if (el) return el;
    el = document.createElement('div');
    el.className = 'modal fade';
    el.id = id;
    el.setAttribute('tabindex', '-1');
    el.setAttribute('aria-hidden', 'true');
    el.innerHTML = ''
      + '<div class="modal-dialog modal-dialog-centered">'
      + '  <div class="modal-content">'
      + '    <div class="modal-header">'
      + '      <h5 class="modal-title">Please wait</h5>'
      + '    </div>'
      + '    <div class="modal-body"><div class="d-flex align-items-center">'
      + '      <div class="spinner-border me-2" role="status" aria-hidden="true"></div>'
      + '      <div class="small flex-fill" id="opendcs-waiting-message">Working...</div>'
      + '    </div></div>'
      + '  </div>'
      + '</div>';
    document.body.appendChild(el);
    return el;
  }
  if (typeof window.show_waiting_modal !== 'function') {
    window.show_waiting_modal = function (title, message) {
      try {
        var el = ensureWaitingModal();
        if (title) el.querySelector('.modal-title').textContent = title;
        if (message) el.querySelector('#opendcs-waiting-message').textContent = message;
        var m = bootstrap.Modal.getOrCreateInstance(el, { backdrop: 'static', keyboard: false });
        m.show();
      } catch (e) {}
    };
  }
  if (typeof window.hide_waiting_modal !== 'function') {
    window.hide_waiting_modal = function () {
      try {
        var el = document.getElementById('opendcs-waiting-modal');
        if (!el) return;
        bootstrap.Modal.getOrCreateInstance(el).hide();
      } catch (e) {}
    };
  }

  // ---------------- Dropdowns in DataTables ----------------
  function bindDropdownToggle(el) {
    if (!el || el.dataset.ddBound === '1') return;
    el.dataset.ddBound = '1';

    // Prevent row click from selecting/opening while still opening the dropdown
    el.addEventListener('click', function (e) {
      e.preventDefault(); //don't navigate for '#' toggles
      e.stopPropagation(); //block row handlers
      try {
        var dd = bootstrap.Dropdown.getOrCreateInstance(el);
        dd.toggle();
      } catch (err) {}
    });

    // shield row handlers that may listen on mousedown
    ['pointerdown','mousedown'].forEach(function(type){
      el.addEventListener(type, function (e) {
        e.stopPropagation();
      });
    });
  }

  function rebindDropdowns(root) {
    (root || document)
        .querySelectorAll('.dataTables_wrapper [data-bs-toggle="dropdown"]')
        .forEach(bindDropdownToggle);
  }

  // Hide dropdown after clicking a menu item, without bubbling to row handlers
  document.addEventListener('click', function (e) {
    var menu = e.target && e.target.closest('.dropdown-menu');
    if (!menu) return;
    e.stopPropagation(); // block row click
    var item = e.target.closest('.dropdown-item, button.dropdown-item');
    if (item) {
      var container = menu.closest('.dropdown, .btn-group');
      var toggle = container && container.querySelector('[data-bs-toggle="dropdown"]');
      if (toggle) {
        // allow the item's own handler to run, then close the menu
        setTimeout(function () {
          try { bootstrap.Dropdown.getOrCreateInstance(toggle).hide(); } catch (err) {}
        }, 0);
      }
    }
  }, true); // capture so we beat row handlers

  whenReady(function () {
    rebindDropdowns(document);

    var mo = new MutationObserver(function (mutations) {
      mutations.forEach(function (m) {
        m.addedNodes && m.addedNodes.forEach(function (n) {
          if (n.nodeType !== 1) return;
          //if (n.matches && n.matches('[data-bs-toggle="dropdown"]')) bindDropdownToggle(n);
          if (n.matches && n.matches('.dataTables_wrapper [data-bs-toggle="dropdown"]')) {
            bindDropdownToggle(n);
          }
          if (n.querySelectorAll) rebindDropdowns(n);
        });
      });
    });
    mo.observe(document.body, { childList: true, subtree: true });
  });

  // ---------------- Sidebar + accordion ----------------
  whenReady(function () {
    var body = document.body;
    var sidebar = document.querySelector('.sidebar.sidebar-main');
    if (sidebar) {
      body.classList.add('has-sidebar');

      // collapse/expand with persistence
      var key = 'opendcs.sidebar.collapsed';
      try { if (localStorage.getItem(key) === '1') body.classList.add('sidebar-collapsed'); } catch (e) {}
      sidebar.querySelectorAll('.sidebar-main-toggle').forEach(function (btn) {
        btn.addEventListener('click', function (e) {
          e.preventDefault();
          body.classList.toggle('sidebar-collapsed');
          try { localStorage.setItem(key, body.classList.contains('sidebar-collapsed') ? '1' : '0'); } catch (e) {}
        });
      });

      // groups accordion (any li that directly contains ul.nav-group-sub)
      function directChild(parent, selector) {
        if (!parent) return null;
        for (var i = 0; i < parent.children.length; i++) {
          var el = parent.children[i];
          if (el.matches && el.matches(selector)) return el;
        }
        return null;
      }
      var lis = Array.prototype.slice.call(sidebar.querySelectorAll('.nav-sidebar > li'));
      var groups = lis.filter(function (li) { return !!directChild(li, 'ul.nav-group-sub'); });

      groups.forEach(function (li) {
        var link = directChild(li, 'a.nav-link');
        var submenu = directChild(li, 'ul.nav-group-sub');
        if (!link || !submenu) return;

        var open = isDisplayed(submenu) || li.classList.contains('nav-item-open') || link.getAttribute('aria-expanded') === 'true';
        if (open) {
          li.classList.add('nav-item-open');
          submenu.style.display = 'block';
        } else {
          li.classList.remove('nav-item-open');
          submenu.style.display = 'none';
        }

        link.addEventListener('click', function (ev) {
          var href = (link.getAttribute('href') || '').trim();
          if (href === '' || href === '#' || href.toLowerCase().indexOf('javascript:') === 0) {
            ev.preventDefault();
          }
          var isOpen = li.classList.contains('nav-item-open');
          groups.forEach(function (sib) {
            if (sib !== li) {
              sib.classList.remove('nav-item-open');
              var s = directChild(sib, 'ul.nav-group-sub'); if (s) s.style.display = 'none';
              var sLink = directChild(sib, 'a.nav-link'); if (sLink) sLink.setAttribute('aria-expanded', 'false');
            }
          });
          if (isOpen) {
            li.classList.remove('nav-item-open');
            submenu.style.display = 'none';
            link.setAttribute('aria-expanded', 'false');
          } else {
            li.classList.add('nav-item-open');
            submenu.style.display = 'block';
            link.setAttribute('aria-expanded', 'true');
          }
        });
      });

      // Open the group that contains an active sub-link, if any
      var active = sidebar.querySelector('.nav-group-sub .nav-link.active');
      if (active) {
        var node = active.closest('li');
        while (node && !directChild(node, 'ul.nav-group-sub')) {
          node = node.parentElement ? node.parentElement.closest('li') : null;
        }
        if (node) {
          groups.forEach(function (sib) {
            if (sib !== node) {
              sib.classList.remove('nav-item-open');
              var s = directChild(sib, 'ul.nav-group-sub'); if (s) s.style.display = 'none';
            }
          });
          node.classList.add('nav-item-open');
          var gs = directChild(node, 'ul.nav-group-sub'); if (gs) gs.style.display = 'block';
        }
      }
    }
  });

  // ---------------- Stacked Modals (BS5) ----------------
  function adjustModalStack(modal) {
    if (modal.parentElement && modal.parentElement !== document.body) {
      document.body.appendChild(modal);
    }
    var openCount = document.querySelectorAll('.modal.show').length;
    var z = 1055 + 10 * openCount;
    modal.style.zIndex = String(z);
    setTimeout(function () {
      var backdrops = document.querySelectorAll('.modal-backdrop');
      for (var i = backdrops.length - 1; i >= 0; i--) {
        var bd = backdrops[i];
        if (!bd.dataset.stackAdjusted) {
          bd.style.zIndex = String(z - 5);
          bd.dataset.stackAdjusted = '1';
          break;
        }
      }
    }, 0);
  }
  document.addEventListener('show.bs.modal', function (e) { adjustModalStack(e.target); });
  document.addEventListener('shown.bs.modal', function (e) { adjustModalStack(e.target); });
  document.addEventListener('hidden.bs.modal', function () {
    if (document.querySelectorAll('.modal.show').length > 0) document.body.classList.add('modal-open');
  });

})();
