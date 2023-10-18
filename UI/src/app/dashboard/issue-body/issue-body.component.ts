import { Component, OnInit ,Input} from '@angular/core';

@Component({
  selector: 'app-issue-body',
  templateUrl: './issue-body.component.html',
  styleUrls: ['./issue-body.component.css']
})
export class IssueBodyComponent implements OnInit {
  @Input() issueData;
  constructor() { }

  ngOnInit(): void {
  }

  convertToHoursIfTime(val, unit) {
    if(val === '-'){
      return val;
    }
    const isLessThanZero = val < 0;
    val = Math.abs(val);
    const hours = (val / (60*60));
    const rhours = Math.floor(hours);
    const minutes = (hours - rhours) * 60;
    const rminutes = Math.round(minutes);
    if (unit?.toLowerCase() === 'day') {
      if (val !== 0) {
        val = this.convertToDays(rminutes, rhours);
      } else {
        val = '0d';
      }
    }
    if (isLessThanZero) {
      val = '-' + val;
    }
    return val;
  }

  convertToHours(rminutes, rhours) {
    if (rminutes === 0) {
      return rhours + 'h';
    } else if (rhours === 0) {
      return rminutes + 'm';
    } else {
      return rhours + 'h ' + rminutes + 'm';
    }
  }

  convertToDays(rminutes, rhours) {
    const days = rhours / 8;
    const rdays = Math.floor(days);
    rhours = (days - rdays) * 8;
    return `${(rdays !== 0) ? rdays + 'd ' : ''}${(rhours !== 0) ? rhours + 'h ' : ''}${(rminutes !== 0) ? rminutes + 'm' : ''}`;
  }

}
