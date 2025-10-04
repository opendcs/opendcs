document.addEventListener("DOMContentLoaded", function(event) {
	console.log("loaded main.js.");

	//This runs the logout functionality.  It is in main.js, which is loaded
	//into any page.
	$(document).on('click', '#logoutButton', function (e) {
		e.preventDefault();
		console.log("Logging out.");
        sessionStorage.removeItem("token");
        window.location = "login";
    });
	
	var token = sessionStorage.getItem("token");
	if (token != null)
	{
		$.ajaxSetup({
		    beforeSend: function(xhr) {
		    	xhr.setRequestHeader('Authorization', 
		    			`${token}`);
		    }
		});
	}
});




/*!
 * Color mode toggler for Bootstrap's docs (https://getbootstrap.com/)
 * Copyright 2011-2025 The Bootstrap Authors
 * Licensed under the Creative Commons Attribution 3.0 Unported License.
 */

(() => {
  'use strict'

  const getStoredTheme = () => localStorage.getItem('theme-mode')
  const setStoredTheme = theme => localStorage.setItem('theme-mode', theme)

  const getPreferredTheme = () => {
    const storedTheme = getStoredTheme()
    if (storedTheme) {
      return storedTheme
    }

    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
  }

  const setTheme = theme => {
    if (theme === 'auto') {
      document.documentElement.setAttribute('data-bs-theme', (window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'))
    } else {
      document.documentElement.setAttribute('data-bs-theme', theme)
    }
  }

  setTheme(getPreferredTheme())

  const showActiveTheme = (theme, focus = false) => {
    const themeSwitcher = document.querySelector('#bd-theme-mode')

    if (!themeSwitcher) {
      return
    }

    const themeSwitcherText = document.querySelector('#bd-theme-mode-text')
    const activeThemeIcon = document.querySelector('.theme-icon-active use')
    const btnToActive = document.querySelector(`[data-bs-theme-value="${theme}"]`)
    const svgOfActiveBtn = btnToActive.querySelector('svg use').getAttribute('href')

    document.querySelectorAll('[data-bs-theme-value]').forEach(element => {
      element.classList.remove('active')
      element.setAttribute('aria-pressed', 'false')
    })

    btnToActive.classList.add('active')
    btnToActive.setAttribute('aria-pressed', 'true')
    activeThemeIcon.setAttribute('href', svgOfActiveBtn)
    const themeSwitcherLabel = `${themeSwitcherText.textContent} (${btnToActive.dataset.bsThemeValue})`
    themeSwitcher.setAttribute('aria-label', themeSwitcherLabel)

    if (focus) {
      themeSwitcher.focus()
    }
  }

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    const storedTheme = getStoredTheme()
    if (storedTheme !== 'light' && storedTheme !== 'dark') {
      setTheme(getPreferredTheme())
    }
  })

  window.addEventListener('DOMContentLoaded', () => {
    showActiveTheme(getPreferredTheme())

    document.querySelectorAll('[data-bs-theme-value]')
      .forEach(toggle => {
        toggle.addEventListener('click', () => {
          const theme = toggle.getAttribute('data-bs-theme-value')
          setStoredTheme(theme)
          setTheme(theme)
          showActiveTheme(theme, true)
        })
      })
  })
})();



// theme link
(() => {
  'use strict'

  const getStoredTheme = () => localStorage.getItem('theme-link')
  const setStoredTheme = theme => localStorage.setItem('theme-link', theme)

  const getPreferredTheme = () => {
    const storedTheme = getStoredTheme()
    if (storedTheme) {
      return storedTheme
    }

    return "/webjars/bootstrap/css/bootstrap.min.css";
  }

  const setTheme = theme => {
	let link = document.getElementById("theme-link");
	link.href = theme;
  }

  setTheme(getPreferredTheme())

  const showActiveTheme = (theme, focus = false) => {
    const themeSwitcher = document.querySelector('#bd-theme-text')

    if (!themeSwitcher) {
      return
    }

    const themeSwitcherText = document.querySelector('#bd-theme-text')
    //const activeThemeIcon = document.querySelector('.theme-icon-active use')
    //const btnToActive = document.querySelector(`[data-bs-theme-value="${theme}"]`)
    //const svgOfActiveBtn = btnToActive.querySelector('svg use').getAttribute('href')

    //document.querySelectorAll('[data-bs-theme-value]').forEach(element => {
    //  element.classList.remove('active')
    //   element.setAttribute('aria-pressed', 'false')
    //})

    // btnToActive.classList.add('active')
    // btnToActive.setAttribute('aria-pressed', 'true')
    // activeThemeIcon.setAttribute('href', svgOfActiveBtn)
    // const themeSwitcherLabel = `${themeSwitcherText.textContent} (${btnToActive.dataset.bsThemeValue})`
    // themeSwitcher.setAttribute('aria-label', themeSwitcherLabel)

    // if (focus) {
    //   themeSwitcher.focus()data-bs-theme-value
    // }
  }

  window.addEventListener('DOMContentLoaded', () => {
    setTheme(getPreferredTheme())

    document.querySelectorAll('[data-bs-theme-link]')
      .forEach(toggle => {
        toggle.addEventListener('click', () => {
          const theme = toggle.getAttribute('data-bs-theme-link')
          setStoredTheme(theme)
          setTheme(theme)
          showActiveTheme(theme, true)
        })
      })
  })
})();


// from https://stackoverflow.com/a/66470962
(function($bs) {
    const CLASS_NAME = 'has-child-dropdown-show';
    $bs.Dropdown.prototype.toggle = function(_orginal) {
        return function() {
            document.querySelectorAll('.' + CLASS_NAME).forEach(function(e) {
                e.classList.remove(CLASS_NAME);
            });
            let dd = this._element.closest('.dropdown').parentNode.closest('.dropdown');
            for (; dd && dd !== document; dd = dd.parentNode.closest('.dropdown')) {
                dd.classList.add(CLASS_NAME);
            }
            return _orginal.call(this);
        }
    }($bs.Dropdown.prototype.toggle);

    document.querySelectorAll('.dropdown').forEach(function(dd) {
        dd.addEventListener('hide.bs.dropdown', function(e) {
            if (this.classList.contains(CLASS_NAME)) {
                this.classList.remove(CLASS_NAME);
                e.preventDefault();
            }
            e.stopPropagation(); // do not need pop in multi level mode
        });
    });


})(bootstrap);
